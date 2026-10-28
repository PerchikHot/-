package com.perchikhot.novagame

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File
import java.util.ArrayDeque
import java.util.zip.ZipInputStream
import java.io.FileOutputStream
import kotlin.random.Random

object ImageLibrary {
    private fun store(ctx:Context)=File(ctx.filesDir,"imported_images")
    fun imported(ctx:Context):List<File> = store(ctx).listFiles()?.filter { it.isFile && it.length()>0 }?.sortedBy { it.name } ?: emptyList()

    // Copies any bundled starter portraits from assets/characters into the gallery once.
    // The APK can therefore ship a ready-to-use collection without requiring a ZIP import.
    fun installBundled(ctx:Context):Int {
        val destination=store(ctx)
        destination.mkdirs()
        val names=try {ctx.assets.list("characters")?.toList().orEmpty()} catch (_:Exception) {emptyList()}
        var added=0
        for(name in names.sorted()) {
            val extension=name.substringAfterLast('.',"").lowercase()
            if(extension !in setOf("jpg","jpeg","png","webp")) continue
            val safeName=name.replace(Regex("[^A-Za-z0-9_.-]"),"_")
            val file=File(destination,"bundled_$safeName")
            if(file.exists() && file.length()>0) continue
            try {
                ctx.assets.open("characters/$name").use {source ->
                    FileOutputStream(file).use {target ->source.copyTo(target)}
                }
                val label=name.substringBeforeLast('.').substringBefore("__").replace('_',' ').trim()
                if(label.isNotBlank()) ctx.getSharedPreferences("image_groups",Context.MODE_PRIVATE).edit().putString(file.name,label).apply()
                added++
            } catch (_:Exception) {file.delete()}
        }
        return added
    }

    fun importZip(ctx:Context,uri:Uri):Int {
        val directory=store(ctx)
        directory.mkdirs()
        var count=0
        // Continue after every existing import, regardless of its extension.
        // This keeps sorted gallery indices (and existing unlocks) stable.
        var nextImageId=directory.listFiles()?.mapNotNull { file ->
            if(file.name.startsWith("image_"))
                file.name.removePrefix("image_").substringBefore('.').toIntOrNull()
            else null
        }?.maxOrNull()?.plus(1) ?: 0
        var total=0L
        ctx.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input.buffered()).use { zip ->
                while(true) {
                    val entry=zip.nextEntry ?: break
                    val ext=entry.name.substringAfterLast('.',"").lowercase()
                    if(!entry.isDirectory && ext in setOf("jpg","jpeg","png","webp")) {
                        if(count>=3000) break
                        // Keep older imports and their gallery group labels intact.
                        var destination=File(directory,"image_%05d.%s".format(nextImageId,ext))
                        while(destination.exists()) {
                            nextImageId++
                            destination=File(directory,"image_%05d.%s".format(nextImageId,ext))
                        }
                        nextImageId++
                        var written=0L
                        try {
                            destination.outputStream().use { out ->
                                val buffer=ByteArray(32768)
                                while(true) {
                                    val n=zip.read(buffer)
                                    if(n<0) break
                                    written+=n
                                    total+=n
                                    if(written>15_000_000L || total>600_000_000L) throw IllegalStateException("Архив слишком большой")
                                    out.write(buffer,0,n)
                                }
                            }
                            count++
                        } catch (error:Exception) {
                            // Remove only the incomplete new file, never an old import.
                            destination.delete()
                            throw error
                        }
                    }
                    zip.closeEntry()
                }
            }
        }
        return count
    }

    fun randomUri(ctx:Context):Uri? {
        val folders=(1..2).mapNotNull {
            ctx.getSharedPreferences("folders",Context.MODE_PRIVATE).getString("folder_$it",null)
        }
        val sources=mutableListOf<()->Uri?>()
        val shop=ctx.getSharedPreferences("unlock_shop",Context.MODE_PRIVATE)
        val available=imported(ctx).filterIndexed {index,file -> file.name.startsWith("bundled_") || index==0 || shop.getBoolean("image_$index",false)}
        if(available.isNotEmpty()) sources.add {
            available.randomOrNull()?.let {Uri.fromFile(it)}
        }
        folders.forEach { folder -> sources.add {randomFromTree(ctx,Uri.parse(folder))} }
        // Do not always prefer imported images: when a local collection exists,
        // the previously selected external folders must still get a chance.
        sources.shuffle()
        for(source in sources) {
            val uri=try {source()} catch (_:Exception) {null}
            if(uri!=null) return uri
        }
        return null
    }

    private fun randomFromTree(ctx:Context,tree:Uri):Uri? {
        val queue=ArrayDeque<String>()
        queue.add(DocumentsContract.getTreeDocumentId(tree))
        var picked:Uri?=null
        var seen=0
        var scanned=0
        while(queue.isNotEmpty() && scanned<40000) {
            val parent=queue.removeFirst()
            val children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,parent)
            ctx.contentResolver.query(children,arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),null,null,null)?.use { cursor ->
                while(cursor.moveToNext() && scanned<40000) {
                    scanned++
                    val id=cursor.getString(0) ?: continue
                    val mime=cursor.getString(1) ?: ""
                    if(mime==DocumentsContract.Document.MIME_TYPE_DIR) queue.add(id)
                    else if(mime.startsWith("image/")) {
                        seen++
                        if(Random.nextInt(seen)==0) picked=DocumentsContract.buildDocumentUriUsingTree(tree,id)
                    }
                }
            }
        }
        return picked
    }
}
