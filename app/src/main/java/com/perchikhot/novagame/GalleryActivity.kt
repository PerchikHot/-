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
    private fun saved(): MutableList<String> = prefs.getString("uris","")!!.split("\n").filter { it.isNotBlank() }.toMutableList()
    private fun save(items:List<String>) { prefs.edit().putString("uris",items.joinToString("\n")).apply() }
    override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState); draw() }
    private fun draw() {
        val scroll=ScrollView(this).apply { setBackgroundColor(Color.rgb(9,17,39)) }
        list=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(18,20,18,20) }
        scroll.addView(list);setContentView(scroll)
        val heading=TextView(this).apply { text="Галерея • ${saved().size} изображений";textSize=24f;setTextColor(Color.WHITE) }
        list.addView(heading)
        list.addView(Button(this).apply { text="Добавить изображения с телефона";setOnClickListener {
            val intent=Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type="image/*";addCategory(Intent.CATEGORY_OPENABLE);putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) }
            startActivityForResult(intent,42)
        } })
        list.addView(TextView(this).apply {text="Файлы остаются на телефоне. Для импорта ZIP сначала распакуй архив в папку с изображениями.";setTextColor(Color.LTGRAY);setPadding(0,12,0,16)})
        saved().forEachIndexed { index, value ->
            val uri=Uri.parse(value)
            val row=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL }
            val image=ImageView(this).apply {
                layoutParams=LinearLayout.LayoutParams(180,180)
                scaleType=ImageView.ScaleType.CENTER_CROP
                try {setImageURI(uri)} catch (_:Exception) {setImageResource(android.R.drawable.ic_menu_report_image)}
                setOnClickListener {
                    val view=ImageView(this@GalleryActivity).apply {setBackgroundColor(Color.BLACK);scaleType=ImageView.ScaleType.FIT_CENTER;setImageURI(uri)}
                    setContentView(view)
                    view.setOnClickListener {draw()}
                }
            }
            row.addView(image)
            row.addView(Button(this).apply {text="Удалить #${index+1}";setOnClickListener {val items=saved();items.remove(value);save(items);draw()} })
            list.addView(row)
        }
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
    override fun onBackPressed(){draw()}
}
