package com.perchikhot.novagame

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.net.Uri
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import android.view.ViewGroup

class GalleryActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("gallery",MODE_PRIVATE) }
    private lateinit var list: LinearLayout
    private var visibleCount=24
    private var savedVisibleCount=50
    private var fullscreenOpen=false
    private val groupPrefs by lazy {getSharedPreferences("image_groups",MODE_PRIVATE)}
    private fun groupOf(name:String)=groupPrefs.getString(name,name.substringBeforeLast(".")) ?: name

    // Persisted document URIs can point to very large camera photos. Decode
    // sampled previews instead of loading full-resolution bitmaps into each row.
    private fun sampledImage(uri:Uri, sampleSize:Int):android.graphics.Bitmap? =
        try {
            contentResolver.openInputStream(uri)?.use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream,null,
                    android.graphics.BitmapFactory.Options().apply { inSampleSize=sampleSize })
            }
        } catch (_:Exception) { null }

    private fun saved(): MutableList<String> = prefs.getString("uris","")!!.split("\n").filter { it.isNotBlank() }.toMutableList()
    private fun save(items:List<String>) { prefs.edit().putString("uris",items.joinToString("\n")).apply() }
    override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState); draw() }
    private fun draw() {
        fullscreenOpen=false
        val scroll=ScrollView(this).apply { setBackgroundColor(Color.rgb(9,17,39)) }
        list=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(18,20,18,20) }
        scroll.addView(list);setContentView(scroll)
        val local=ImageLibrary.imported(this)
        val savedItems=saved()
        val savedHeading=TextView(this).apply {
            text="С ТЕЛЕФОНА • ${savedItems.size} изображений"
            textSize=19f;setTextColor(Color.WHITE);setPadding(0,16,0,12)
        }
        val heading=TextView(this).apply { text="Галерея • ${savedItems.size+local.size} изображений";textSize=24f;setTextColor(Color.WHITE) }
        list.addView(heading)
        if (local.isNotEmpty() || savedItems.isNotEmpty()) {
            list.addView(Button(this).apply {
                text="🎲 Случайное изображение"
                setOnClickListener {
                    // Both imported and user-selected photos can appear here.
                    val candidates=local.map { Uri.fromFile(it) } + savedItems.map { Uri.parse(it) }
                    val uri=candidates.random()
                    val bitmap=sampledImage(uri,4)
                    if (bitmap==null) {
                        Toast.makeText(this@GalleryActivity,"Изображение недоступно",Toast.LENGTH_SHORT).show()
                    } else {
                        val full=ImageView(this@GalleryActivity).apply {
                            setBackgroundColor(Color.BLACK)
                            scaleType=ImageView.ScaleType.FIT_CENTER
                            setImageBitmap(bitmap)
                            setOnClickListener { draw() }
                        }
                        fullscreenOpen=true
                        setContentView(full)
                    }
                }
            })
        }
        list.addView(Button(this).apply { text="Добавить изображения с телефона";setOnClickListener {
            val intent=Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type="image/*";addCategory(Intent.CATEGORY_OPENABLE);putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) }
            startActivityForResult(intent,42)
        } })
        list.addView(TextView(this).apply {text="Файлы остаются на телефоне. Для импорта ZIP сначала распакуй архив в папку с изображениями.";setTextColor(Color.LTGRAY);setPadding(0,12,0,16)})
        if(savedItems.isNotEmpty()) list.addView(Button(this).apply {
            text="↓ К изображениям с телефона (${savedItems.size})"
            setOnClickListener { savedHeading.post { scroll.smoothScrollTo(0,savedHeading.top) } }
        })
        if(local.isNotEmpty()) {
            val groups=local.groupBy {groupOf(it.name)}.toSortedMap()
            list.addView(TextView(this).apply {
                text="ПЕРСОНАЖИ • ${groups.size} героинь / ${local.size} изображений"
                textSize=19f;setTextColor(Color.WHITE);setPadding(0,16,0,12)
            })
            groups.entries.take(visibleCount).forEach { (name,photos) ->
                val section=LinearLayout(this).apply {
                    orientation=LinearLayout.VERTICAL;setPadding(8,12,8,12)
                    setBackgroundColor(Color.rgb(23,33,62))
                }
                val title=TextView(this).apply {
                    text="$name • ${photos.size} фото";textSize=19f;setTextColor(Color.WHITE)
                    setPadding(8,4,8,8)
                }
                section.addView(title)
                val cover=ImageView(this).apply {
                    scaleType=ImageView.ScaleType.CENTER_CROP
                    setImageBitmap(android.graphics.BitmapFactory.decodeFile(photos.first().absolutePath,
                        android.graphics.BitmapFactory.Options().apply {inSampleSize=8}))
                }
                section.addView(cover,LinearLayout.LayoutParams(-1,220))
                val gallery=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;visibility=android.view.View.GONE}
                // Decode at most 24 thumbnails per tap to avoid exhausting
                // memory when a character has hundreds of imported photos.
                var loadedPhotos=0
                fun populateGallery() {
                val batch=photos.drop(loadedPhotos).take(24)
                batch.forEach {file ->
                    val photo=ImageView(this).apply {
                        scaleType=ImageView.ScaleType.CENTER_CROP
                        setImageBitmap(android.graphics.BitmapFactory.decodeFile(file.absolutePath,
                            android.graphics.BitmapFactory.Options().apply {inSampleSize=8}))
                        setOnClickListener {
                            val full=ImageView(this@GalleryActivity).apply {
                                setBackgroundColor(Color.BLACK);scaleType=ImageView.ScaleType.FIT_CENTER
                                setImageBitmap(android.graphics.BitmapFactory.decodeFile(file.absolutePath,
                                    android.graphics.BitmapFactory.Options().apply {inSampleSize=2}))
                                setOnClickListener {draw()}
                            }
                            fullscreenOpen=true
                            setContentView(full)
                        }
                    }
                    gallery.addView(photo,LinearLayout.LayoutParams(-1,260))
                    gallery.addView(Button(this).apply {
                        text="Переместить изображение"
                        setOnClickListener {
                            val options=(listOf("＋ Новая героиня")+groups.keys.filter {it!=name}).toTypedArray()
                            android.app.AlertDialog.Builder(this@GalleryActivity)
                                .setTitle("К какому персонажу относится фото?")
                                .setItems(options) {_,which ->
                                    if(which==0) {
                                        val field=EditText(this@GalleryActivity).apply {hint="Имя героини";setSingleLine(true)}
                                        android.app.AlertDialog.Builder(this@GalleryActivity).setTitle("Новая героиня")
                                            .setView(field).setPositiveButton("Сохранить") {_,_->
                                                val entered=field.text.toString().trim()
                                                if(entered.isNotEmpty()) {groupPrefs.edit().putString(file.name,entered).apply();draw()}
                                            }.setNegativeButton("Отмена",null).show()
                                    } else {groupPrefs.edit().putString(file.name,options[which]).apply();draw()}
                                }.setNegativeButton("Отмена",null).show()
                        }
                    })
                }
                loadedPhotos+=batch.size
                if(loadedPhotos<photos.size) {
                    gallery.addView(Button(this).apply {
                        text="Показать ещё 24 фото (${photos.size-loadedPhotos} осталось)"
                        setOnClickListener {
                            gallery.removeView(this)
                            populateGallery()
                        }
                    })
                }
                }
                section.addView(Button(this).apply {
                    text="Открыть изображения (${photos.size})"
                    setOnClickListener {
                        if (gallery.childCount == 0) populateGallery()
                        gallery.visibility=if(gallery.visibility==android.view.View.VISIBLE) android.view.View.GONE else android.view.View.VISIBLE
                        text=if(gallery.visibility==android.view.View.VISIBLE) "Свернуть" else "Открыть изображения (${photos.size})"
                    }
                })
                section.addView(Button(this).apply {
                    text="Переименовать героиню"
                    setOnClickListener {
                        val field=EditText(this@GalleryActivity).apply {setText(name);setSingleLine(true)}
                        android.app.AlertDialog.Builder(this@GalleryActivity).setTitle("Имя персонажа")
                            .setView(field).setPositiveButton("Сохранить") {_,_->
                                val newName=field.text.toString().trim()
                                if(newName.isNotEmpty()) {
                                    val editor=groupPrefs.edit()
                                    photos.forEach {editor.putString(it.name,newName)}
                                    editor.apply();draw()
                                }
                            }.setNegativeButton("Отмена",null).show()
                    }
                })
                section.addView(gallery)
                list.addView(section)
            }
            if(visibleCount<groups.size) list.addView(Button(this).apply {
                text="Показать ещё 24 персонажа";setOnClickListener {visibleCount+=24;draw()}
            })
        }
        if(savedItems.isNotEmpty()) list.addView(savedHeading)
        savedItems.take(savedVisibleCount).forEachIndexed { index, value ->
            val uri=Uri.parse(value)
            val row=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL }
            val image=ImageView(this).apply {
                layoutParams=LinearLayout.LayoutParams(180,180)
                scaleType=ImageView.ScaleType.CENTER_CROP
                val preview=sampledImage(uri,8)
                if(preview!=null) setImageBitmap(preview) else setImageResource(android.R.drawable.ic_menu_report_image)
                setOnClickListener {
                    val view=ImageView(this@GalleryActivity).apply {setBackgroundColor(Color.BLACK);scaleType=ImageView.ScaleType.FIT_CENTER
                        val preview=sampledImage(uri,2)
                        if(preview!=null) setImageBitmap(preview) else setImageResource(android.R.drawable.ic_menu_report_image)}
                    fullscreenOpen=true
                    setContentView(view)
                    view.setOnClickListener {draw()}
                }
            }
            row.addView(image)
            row.addView(Button(this).apply {text="Удалить #${index+1}";setOnClickListener {val items=saved();items.remove(value);save(items);draw()} })
            list.addView(row)
        }
        if(savedVisibleCount<savedItems.size) list.addView(Button(this).apply {
            text="Показать ещё изображения (${savedItems.size-savedVisibleCount} осталось)"
            setOnClickListener {savedVisibleCount+=50;draw()}
        })
    }
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode!=42 || resultCode!=RESULT_OK || data==null) return
        val items=saved()
        val uris=mutableListOf<Uri>()
        data.data?.let {uris.add(it)}
        data.clipData?.let {clip->for(i in 0 until clip.itemCount) uris.add(clip.getItemAt(i).uri)}
        uris.forEach { uri->
            try {contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)} catch (_:Exception) {}
            if(uri.toString() !in items) items.add(uri.toString())
        }
        save(items);draw()
    }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed(){
        if(fullscreenOpen) draw() else super.onBackPressed()
    }
}
