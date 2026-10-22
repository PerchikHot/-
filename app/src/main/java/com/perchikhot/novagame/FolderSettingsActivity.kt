package com.perchikhot.novagame

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.graphics.Color
import android.widget.*

class FolderSettingsActivity:Activity() {
 private val prefs by lazy {getSharedPreferences("folders",MODE_PRIVATE)}
 private lateinit var root:LinearLayout
 override fun onCreate(savedInstanceState:Bundle?) {super.onCreate(savedInstanceState);show()}
 private fun show() {
  root=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;setPadding(24,32,24,16);setBackgroundColor(Color.rgb(9,17,39))}
  setContentView(root)
  text("Коллекции изображений",24)
  text("Подключи две разные папки. Изображения выбираются случайно из обеих папок.")
  for(i in 1..2) {
   val uri=prefs.getString("folder_$i",null)
   text("Коллекция $i: "+(uri?.let{Uri.parse(it).lastPathSegment} ?: "не выбрана"))
   root.addView(Button(this).apply{text="Выбрать папку $i";setOnClickListener {
    val intent=Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)}
    startActivityForResult(intent,i)
   }})

  }
  text("Из ZIP можно загрузить изображения прямо в приложение, без публикации в GitHub.")
  root.addView(Button(this).apply {text="Импортировать ZIP с изображениями";setOnClickListener {
   val intent=Intent(Intent.ACTION_OPEN_DOCUMENT).apply {type="application/zip";addCategory(Intent.CATEGORY_OPENABLE)}
   startActivityForResult(intent,50)
  }})
  text("Уже импортировано: ${ImageLibrary.imported(this).size} изображений")
  text("Вложенные папки поддерживаются. Видео пропускаются.")
 }
 private fun text(value:String,size:Int=17) {root.addView(TextView(this).apply{text=value;textSize=size.toFloat();setTextColor(Color.WHITE);setPadding(0,10,0,10)})}
 override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
  super.onActivityResult(requestCode,resultCode,data)
  if(resultCode==RESULT_OK && requestCode==50) {
   val uri=data?.data ?: return
   text("Импортируем архив…")
   Thread {
    val result=try { "Импортировано: ${ImageLibrary.importZip(this,uri)} изображений" } catch(e:Exception) {"Ошибка импорта: ${e.message}"}
    runOnUiThread {Toast.makeText(this,result,Toast.LENGTH_LONG).show();show()}
   }.start()
   return
  }
  if(resultCode==RESULT_OK && requestCode in 1..2) {
   data?.data?.let {uri->
    try {contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)} catch (_:Exception){}
    prefs.edit().putString("folder_$requestCode",uri.toString()).apply()
   }
  }
  show()
 }
}
