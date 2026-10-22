package com.perchikhot.novagame

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*
import java.io.File

class MenuActivity:Activity() {
    private val handler=Handler(Looper.getMainLooper())
    private lateinit var frame:FrameLayout
    private lateinit var background:ImageView
    private var screenName="start"
    private var pictureIndex=0
    private var imageRequest=0
    private val prefs by lazy {getSharedPreferences("unlock_shop",MODE_PRIVATE)}
    private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
    private val cycle=object:Runnable {
        override fun run() {
            if(!isFinishing && (screenName=="start" || screenName=="modes")) {
                changeBackground()
                handler.postDelayed(this,11000)
            }
        }
    }
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        showStart()
    }
    override fun onResume() {
        super.onResume()
        if(screenName=="journey") showJourney()
        handler.removeCallbacks(cycle)
        handler.postDelayed(cycle,11000)
    }
    override fun onPause() {
        handler.removeCallbacks(cycle)
        super.onPause()
    }
    private fun unlocked():List<File> {
        val images=ImageLibrary.imported(this)
        return images.filterIndexed {i,_ -> i==0 || prefs.getBoolean("image_$i",false)}
    }
    private fun changeBackground() {
        val files=unlocked()
        if(files.isEmpty()) {
            background.setBackgroundColor(Color.rgb(20,17,49))
            background.setImageDrawable(null)
            return
        }
        pictureIndex=(pictureIndex+1)%files.size
        val target=files[pictureIndex]
        val request=++imageRequest
        Thread {
            val bitmap=BitmapFactory.decodeFile(target.absolutePath,BitmapFactory.Options().apply {inSampleSize=1})
            runOnUiThread {
                if(!isFinishing && request==imageRequest && ::background.isInitialized && bitmap!=null) {
                    background.animate().alpha(0.25f).setDuration(280).withEndAction {
                        background.setImageBitmap(bitmap)
                        background.animate().alpha(1f).setDuration(700).start()
                    }.start()
                }
            }
        }.start()
    }
    private fun setup(name:String) {
        screenName=name
        imageRequest++
        frame=FrameLayout(this)
        frame.setBackgroundColor(Color.rgb(10,12,36))
        background=ImageView(this).apply {scaleType=ImageView.ScaleType.CENTER_CROP}
        frame.addView(background,FrameLayout.LayoutParams(-1,-1))
        val shade=View(this).apply {
            background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0x55080B22,0x220C0C2C,0x99070A21.toInt()))
        }
        frame.addView(shade,FrameLayout.LayoutParams(-1,-1))
        setContentView(frame)
        changeBackground()
    }
    private fun overlay():LinearLayout {
        val scroll=ScrollView(this).apply {isFillViewport=true}
        frame.addView(scroll,FrameLayout.LayoutParams(-1,-1))
        return LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            gravity=Gravity.CENTER_HORIZONTAL
            setPadding(dp(24),dp(38),dp(24),dp(30))
            scroll.addView(this)
        }
    }
    private fun text(container:LinearLayout,value:String,size:Int=20):TextView {
        return TextView(this).apply {
            text=value;textSize=size.toFloat();setTextColor(Color.WHITE);gravity=Gravity.CENTER
            setShadowLayer(3f,0f,2f,Color.BLACK)
            setPadding(dp(6),dp(10),dp(6),dp(10))
            container.addView(this)
        }
    }
    private fun action(container:LinearLayout,title:String,subtitle:String="",onClick:()->Unit) {
        val item=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER
            setPadding(dp(10),dp(10),dp(10),dp(10))
            background=GradientDrawable().apply {
                cornerRadius=dp(17).toFloat()
                setColor(0xA6222457.toInt())
                setStroke(dp(1),0xFF9684E7.toInt())
            }
            setOnClickListener {
                animate().scaleX(.97f).scaleY(.97f).setDuration(110).withEndAction {
                    animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                    onClick()
                }.start()
            }
        }
        text(item,title,21)
        if(subtitle.isNotBlank()) text(item,subtitle,13)
        container.addView(item,LinearLayout.LayoutParams(-1,-2).apply {bottomMargin=dp(13)})
    }
    private fun showStart() {
        setup("start")
        val col=overlay()
        col.addView(View(this),LinearLayout.LayoutParams(1,0,1f))
        text(col,"✦ NOVA GAME ✦",34)
        text(col,"ТВОЯ КОЛЛЕКЦИЯ • ТВОИ ИСТОРИИ",14)
        col.addView(View(this),LinearLayout.LayoutParams(1,dp(45)))
        action(col,"▶  ИГРАТЬ") {
            showModes()
        }
        action(col,"🎮  ТРИ ИГРОВЫХ ПРОТОТИПА","Попробуй разные ощущения от игры") {showPrototypes()}
        action(col,"✦  ПУТЬ КОЛЛЕКЦИОНЕРА","Уровни, звёзды и новые открытия") {showJourney()}
        action(col,"✦  МАГАЗИН ПЕРСОНАЖЕЙ") {showShop()}
        action(col,"⚙  КОЛЛЕКЦИИ") {startActivity(Intent(this,FolderSettingsActivity::class.java))}
        action(col,"⏻  ВЫХОД") {finishAffinity()}
        col.addView(View(this),LinearLayout.LayoutParams(1,0,1f))
        text(col,"Фон меняется из открытых изображений",12)
    }
    private fun showPrototypes() {
        setup("prototypes")
        val col=overlay()
        text(col,"ИСПЫТАТЕЛЬНАЯ ЛАБОРАТОРИЯ",27)
        text(col,"Три подхода к NOVA GAME. Попробуй каждый и выбери самый интересный.",16)
        action(col,"⚡ АРКАДА: ОХОТА ЗА КОМБО","35 элементов, ограничение по ходам") {
            startActivity(Intent(this,MainActivity::class.java).putExtra("mode","match3").putExtra("prototype","arcade"))
        }
        action(col,"🧠 ТАКТИКА: ПАМЯТЬ","Найди пары с минимальным числом ошибок") {
            startActivity(Intent(this,MainActivity::class.java).putExtra("mode","memory").putExtra("prototype","tactics"))
        }
        action(col,"✦ ОТКРЫТИЕ: ТРИ ЗВЕЗДЫ","Девять попыток, награда и изображение") {
            startActivity(Intent(this,MainActivity::class.java).putExtra("mode","reveal").putExtra("prototype","discovery"))
        }
        action(col,"← НАЗАД") {showStart()}
    }
    private fun showJourney() {
        setup("journey")
        val col=overlay()
        text(col,"✦ ПУТЬ КОЛЛЕКЦИОНЕРА",28)
        val wins=getSharedPreferences("progress",MODE_PRIVATE).getInt("wins",0)
        val games=listOf(
            Triple("ФОТО-ПАЗЛЫ","puzzle","Собери изображение"),
            Triple("ПАМЯТЬ","memory","Найди все пары"),
            Triple("ТРИ В РЯД","match3","Создай комбинации"),
            Triple("НАЙДИ ОТЛИЧИЯ","differences","Проверь внимательность"),
            Triple("ОТКРОЙ ИЗОБРАЖЕНИЕ","reveal","Раскрой картинку")
        )
        text(col,"Пройдено заданий: $wins  •  Очки открывают новые фото",16)
        val wallet=getSharedPreferences("MainActivity",MODE_PRIVATE).getInt("score",0)
        val images=ImageLibrary.imported(this)
        val unlockedCount=images.indices.count { it==0 || prefs.getBoolean("image_$it",false) }
        text(col,"★ $wallet очков   •   🖼 $unlockedCount / ${images.size} изображений",16)
        val progressBar=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply {
            max=5
            progress=wins%5
            progressTintList=android.content.res.ColorStateList.valueOf(0xFFDD80F5.toInt())
            progressBackgroundTintList=android.content.res.ColorStateList.valueOf(0x66444466)
        }
        col.addView(progressBar,LinearLayout.LayoutParams(-1,dp(9)).apply {bottomMargin=dp(10)})
        text(col,"До следующей серии: ${5-wins%5} уровней",14)
        text(col,"КАРТА ПРИКЛЮЧЕНИЯ",18)
        for(index in 0 until minOf(15,wins+5)) {
            val game=games[index%games.size]
            val available=index<=wins
            val completed=index<wins
            val icon=if(completed) "✓" else if(available) "▶" else "🔒"
            val title="$icon  ${index+1}. ${game.first}"
            val subtitle=if(completed) "Пройдено • + награда" else if(available) game.third else "Откроется после предыдущего уровня"
            action(col,title,subtitle) {
                if(available && !completed) {
                    startActivity(Intent(this,MainActivity::class.java).putExtra("mode",game.second).putExtra("journey",true))
                } else if(!available) Toast.makeText(this,"Сначала пройди уровень ${wins+1}",Toast.LENGTH_SHORT).show()
            }
        }
        action(col,"★ МАГАЗИН НАГРАД","Обменяй очки на изображения") {showShop()}
        action(col,"← НАЗАД") {showStart()}
    }
    private fun showModes() {
        setup("modes")
        val col=overlay()
        text(col,"ВЫБЕРИ РЕЖИМ",28)
        col.addView(View(this),LinearLayout.LayoutParams(1,dp(30)))
        val games=listOf(
            Triple("🧩  ФОТО-ПАЗЛЫ","Собери картинку","puzzle"),
            Triple("🃏  ПАМЯТЬ","Найди пары","memory"),
            Triple("💎  ТРИ В РЯД","Собирай комбинации","match3"),
            Triple("🔎  НАЙДИ ОТЛИЧИЯ","Сравни изображения","differences"),
            Triple("✨  ОТКРЫТЬ ПЛИТКИ","Открывай фотографии","reveal")
        )
        games.forEach { (name,description,mode) ->
            action(col,name,description) {
                startActivity(Intent(this,MainActivity::class.java).putExtra("mode",mode))
            }
        }
        action(col,"★  МАГАЗИН ПЕРСОНАЖЕЙ") {showShop()}
        action(col,"🖼  ГАЛЕРЕЯ") {startActivity(Intent(this,GalleryActivity::class.java))}
        action(col,"←  НАЗАД") {showStart()}
    }
    private fun showShop() {
        setup("shop")
        val col=overlay()
        text(col,"МАГАЗИН ПЕРСОНАЖЕЙ",26)
        val points=getSharedPreferences("MainActivity",MODE_PRIVATE).getInt("score",0)
        text(col,"★ $points очков • Открой фото для фонов и игр",16)
        action(col,"←  НАЗАД") {showStart()}
        val files=ImageLibrary.imported(this)
        if(files.isEmpty()) {
            text(col,"Сначала импортируй ZIP через «Коллекции».",17)
            action(col,"📁  ИМПОРТИРОВАТЬ ZIP") {startActivity(Intent(this,FolderSettingsActivity::class.java))}
            return
        }
        val grid=GridLayout(this).apply {columnCount=3}
        col.addView(grid)
        files.take(90).forEachIndexed {index,file ->
            val open=index==0 || prefs.getBoolean("image_$index",false)
            val cell=LinearLayout(this).apply {
                orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER
                setPadding(dp(3),dp(3),dp(3),dp(8))
            }
            val preview=ImageView(this).apply {
                scaleType=ImageView.ScaleType.CENTER_CROP
                if(open) setImageBitmap(BitmapFactory.decodeFile(file.absolutePath,BitmapFactory.Options().apply {inSampleSize=8}))
                else setBackgroundColor(0xFF27254D.toInt())
            }
            cell.addView(preview,LinearLayout.LayoutParams(-1,dp(115)))
            val cost=100
            text(cell,if(open)"✓ Открыто" else "🔒 $cost ★",12)
            cell.setOnClickListener {
                if(open) {
                    android.app.AlertDialog.Builder(this).setTitle("Изображение №${index+1}")
                        .setMessage("Это изображение доступно в фонах и играх.")
                        .setPositiveButton("OK",null).show()
                } else {
                    val wallet=getSharedPreferences("MainActivity",MODE_PRIVATE)
                    val current=wallet.getInt("score",0)
                    if(current>=cost) {
                        wallet.edit().putInt("score",current-cost).apply()
                        prefs.edit().putBoolean("image_$index",true).apply()
                        showShop()
                    } else Toast.makeText(this,"Недостаточно очков",Toast.LENGTH_SHORT).show()
                }
            }
            val width=(resources.displayMetrics.widthPixels-dp(48))/3
            grid.addView(cell,GridLayout.LayoutParams().apply {this.width=width;this.height=dp(162)})
        }
        if(files.size>90) text(col,"Пока показаны первые 90 изображений. Остальные добавим постранично.",13)
    }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when(screenName) {"start"->finish(); "shop"->showStart(); else->showStart()}
    }
}
