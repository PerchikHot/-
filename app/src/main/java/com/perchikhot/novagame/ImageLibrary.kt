package com.perchikhot.novagame

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File
import java.util.ArrayDeque
import java.util.zip.ZipInputStream
import kotlin.random.Random

object ImageLibrary {
    private fun store(ctx:Context)=File(ctx.filesDir,"imported_images")
    fun imported(ctx:Context):List<File> = store(ctx).listFiles()?.filter { it.isFile && it.length()>0 } ?: emptyList()

    fun importZip(ctx:Context,uri:Uri):Int {
        val directory=store(ctx)
        directory.mkdirs()
        var count=0
        var total=0L
        ctx.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input.buffered()).use { zip ->
                while(true) {
                    val entry=zip.nextEntry ?: break
                    val ext=entry.name.substringAfterLast('.',"").lowercase()
                    if(!entry.isDirectory && ext in setOf("jpg","jpeg","png","webp")) {
                        if(count>=3000) break
                        val destination=File(directory,"image_%05d.%s".format(count,ext))
                        var written=0L
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
        if(imported(ctx).isNotEmpty()) sources.add {
            imported(ctx).randomOrNull()?.let {Uri.fromFile(it)}
        }
        folders.forEach { folder -> sources.add {randomFromTree(ctx,Uri.parse(folder))} }
        for(source in sources.shuffled()) {
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
