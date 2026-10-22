package com.perchikhot.novagame

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.*
import kotlin.random.Random

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private var score = 0
    private val bg = Color.rgb(9, 17, 39)
    private val blue = Color.rgb(41, 101, 201)
    private val symbols = listOf("★","◆","♥","☀","♣","✿")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        score = getPreferences(MODE_PRIVATE).getInt("score",0)
        if ((1..2).any { getSharedPreferences("folders",MODE_PRIVATE).getString("folder_$it",null)==null }) {
            startActivity(android.content.Intent(this,FolderSettingsActivity::class.java))
        }
        home()
    }
    private fun screen(title: String) {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 30, 24, 16)
            setBackgroundColor(bg)
        }
        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
        label(title, 26)
    }
    private fun label(s:String, size:Int=18) {
        root.addView(TextView(this).apply {
            text=s; textSize=size.toFloat(); setTextColor(Color.WHITE)
            gravity=Gravity.CENTER; setPadding(8,14,8,14)
        })
    }
    private fun button(s:String, action:()->Unit): Button {
        return Button(this).apply {
            text=s; textSize=17f; setTextColor(Color.WHITE)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(blue))
            setOnClickListener { action() }
        }
    }
    private fun add(s:String, action:()->Unit) { root.addView(button(s,action)) }
    private fun award(points:Int=10) {
        score+=points
        getPreferences(MODE_PRIVATE).edit().putInt("score",score).apply()
        Toast.makeText(this,"+$points очков! Всего: $score",Toast.LENGTH_SHORT).show()
    }
    private fun back() = add("← Главное меню") { home() }
    private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
    private fun panel(color:Int, radius:Int=22):android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
            intArrayOf(color,Color.rgb(17,24,53))
        ).apply {
            cornerRadius=dp(radius).toFloat()
            setStroke(dp(1),Color.rgb(83,68,139))
        }
    private fun gameCard(icon:String,title:String,subtitle:String,color:Int,action:()->Unit) {
        val card=LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL
            gravity=Gravity.CENTER_VERTICAL
            setPadding(dp(16),dp(17),dp(12),dp(17))
            background=panel(color)
            elevation=dp(4).toFloat()
        }
        card.addView(TextView(this).apply {
            text=icon; textSize=31f; gravity=Gravity.CENTER
        },LinearLayout.LayoutParams(dp(57),dp(64)))
        val content=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL}
        content.addView(TextView(this).apply {
            text=title; textSize=20f; setTextColor(Color.WHITE)
            setTypeface(null,android.graphics.Typeface.BOLD)
        })
        content.addView(TextView(this).apply {
            text=subtitle; textSize=12f; setTextColor(Color.rgb(189,190,218))
            setPadding(0,dp(5),0,0)
        })
        card.addView(content,LinearLayout.LayoutParams(0,-2,1f))
        card.addView(TextView(this).apply {
            text="›";textSize=30f;setTextColor(Color.rgb(203,179,255))
        })
        card.setOnClickListener { action() }
        card.setOnTouchListener { view,event ->
            when(event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {view.animate().scaleX(.98f).scaleY(.98f).setDuration(90).start();false}
                android.view.MotionEvent.ACTION_UP,android.view.MotionEvent.ACTION_CANCEL -> {view.animate().scaleX(1f).scaleY(1f).setDuration(130).start();false}
                else -> false
            }
        }
        root.addView(card,LinearLayout.LayoutParams(-1,-2).apply {bottomMargin=dp(12)})
    }
    private fun home() {
        screen("✦ NOVA GAME ✦")
        label("ТВОЯ КОЛЛЕКЦИЯ • ТВОИ ИСТОРИИ",13)
        val banner=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(dp(18),dp(18),dp(18),dp(18))
            background=panel(Color.rgb(67,38,118),26)
        }
        banner.addView(TextView(this).apply {
            text="✧  ГАЛЕРЕЯ ОТКРЫТИЙ";textSize=21f
            setTypeface(null,android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE)
        })
        banner.addView(TextView(this).apply {
            text="Зарабатывай очки и открывай изображения из своих коллекций"
            textSize=14f;setTextColor(Color.rgb(220,211,244));setPadding(0,dp(10),0,dp(10))
        })
        banner.addView(TextView(this).apply {
            text="✦  $score очков";textSize=19f;setTextColor(Color.rgb(250,210,255))
        })
        banner.setOnClickListener { gallery() }
        root.addView(banner,LinearLayout.LayoutParams(-1,-2).apply {bottomMargin=dp(22)})
        label("ВЫБЕРИ ИГРУ",18)
        gameCard("🧩","Пазлы","Собери головоломку",Color.rgb(44,47,108)) {puzzle()}
        gameCard("🃏","Память","Найди одинаковые пары",Color.rgb(60,40,103)) {memory()}
        gameCard("💎","Три в ряд","Создавай комбинации",Color.rgb(34,65,109)) {match3()}
        gameCard("🔎","Найди отличия","Проверь внимательность",Color.rgb(67,45,99)) {differences()}
        gameCard("✨","Открыть плитки","Открой случайное изображение",Color.rgb(84,41,102)) {reveal()}
        label("КОЛЛЕКЦИЯ",18)
        gameCard("🖼","Галерея","Просмотр изображений",Color.rgb(39,56,86)) {gallery()}
        gameCard("📁","Мои папки","Настройка двух коллекций",Color.rgb(39,56,86)) {
            startActivity(android.content.Intent(this,FolderSettingsActivity::class.java))
        }
    }
    private fun grid(cols:Int, cells:Int, onCell:(Int,Button)->Unit):List<Button> {
        val buttons=mutableListOf<Button>()
        for (r in 0 until (cells+cols-1)/cols) {
            val row=LinearLayout(this)
            root.addView(row)
            for(c in 0 until cols) {
                val index=r*cols+c
                if(index>=cells) break
                val b=button("?",{}) 
                row.addView(b,LinearLayout.LayoutParams(0,(64*resources.displayMetrics.density).toInt(),1f))
                b.minHeight=0; b.setPadding(0,0,0,0); b.textSize=20f
                b.setOnClickListener { onCell(index,b) }
                buttons.add(b)
            }
        }
        return buttons
    }
    private fun memory() {
        screen("Память")
        label("Найди 6 одинаковых пар")
        val values=(symbols+symbols).shuffled()
        val opened=mutableSetOf<Int>()
        var first=-1
        var locked=false
        lateinit var buttons:List<Button>
        buttons=grid(3,12) { i,b ->
            if(!locked && i !in opened && i!=first) {
                b.text=values[i]
                if(first<0) first=i else {
                    val prev=first; first=-1
                    if(values[prev]==values[i]) {
                        opened.add(prev);opened.add(i)
                        if(opened.size==12) { award(); label("Победа! Все пары найдены.") }
                    } else {
                        locked=true
                        b.postDelayed({buttons[prev].text="?";buttons[i].text="?";locked=false},750)
                    }
                }
            }
        }
        back()
    }
    private fun puzzle() {
        screen("Пазлы")
        label("Расставь числа от 1 до 8. Нажми на плитку рядом с пустой.")
        val cells=(1..8).toMutableList().apply { add(0) }
        repeat(100) {
            val z=cells.indexOf(0)
            val n=listOf(z-3,z+3,if(z%3>0)z-1 else -1,if(z%3<2)z+1 else -1).filter{it in 0..8}.random()
            cells[z]=cells[n];cells[n]=0
        }
        lateinit var buttons:List<Button>
        buttons=grid(3,9) { i,_ ->
            val z=cells.indexOf(0)
            if((i/3==z/3 && kotlin.math.abs(i-z)==1)||kotlin.math.abs(i-z)==3) {
                cells[z]=cells[i];cells[i]=0
                buttons.forEachIndexed{idx,b->b.text=if(cells[idx]==0)" " else cells[idx].toString()}
                if(cells==(1..8).toList()+0) {award();label("Пазл собран!")}
            }
        }
        buttons.forEachIndexed{idx,b->b.text=if(cells[idx]==0)" " else cells[idx].toString()}
        back()
    }
    private fun match3() {
        screen("Три в ряд")
        label("Нажми две соседние клетки, чтобы поменять их местами и собрать три одинаковых.")
        val tiles=MutableList(36){Random.nextInt(4)}
        val icons=listOf("💎","⭐","🔷","💜")
        var selected=-1
        var moves=0
        lateinit var buttons:List<Button>
        fun render(){buttons.forEachIndexed{i,b->b.text=icons[tiles[i]]}}
        fun clearMatches():Int {
            val matches=mutableSetOf<Int>()
            for(i in 0..35) {
                if(i%6<=3 && tiles[i]==tiles[i+1] && tiles[i]==tiles[i+2]) matches.addAll(listOf(i,i+1,i+2))
                if(i/6<=3 && tiles[i]==tiles[i+6] && tiles[i]==tiles[i+12]) matches.addAll(listOf(i,i+6,i+12))
            }
            matches.forEach{tiles[it]=Random.nextInt(4)}
            return matches.size
        }
        buttons=grid(6,36){i,_->
            if(selected<0) selected=i else {
                val j=selected;selected=-1
                if((i/6==j/6 && kotlin.math.abs(i-j)==1)||kotlin.math.abs(i-j)==6){
                    val t=tiles[i];tiles[i]=tiles[j];tiles[j]=t
                    val removed=clearMatches()
                    if(removed>0) award(removed*2)
                    moves++
                    render()
                }
            }
        }
        render()
        back()
    }
    private fun differences() {
        screen("Найди отличия")
        label("Сверху показаны 8 символов. Сравни их с клетками ниже в том же порядке: слева направо, сначала верхний ряд. Нажми на 3 клетки, где символ изменился.")
        val base=List(8){symbols.random()}
        val changed=base.toMutableList()
        val positions=(0..7).shuffled().take(3).toSet()
        positions.forEach{i->changed[i]=symbols.filter{it!=base[i]}.random()}
        label("Образец: "+base.joinToString(" "))
        label("Найди отличия в строке ниже:")
        val found=mutableSetOf<Int>()
        grid(4,8){i,b->
            if(i in positions && found.add(i)){
                b.text="✓"
                if(found.size==3){award();label("Все отличия найдены!")}
            } else if(i !in positions) Toast.makeText(this,"Совпадает",Toast.LENGTH_SHORT).show()
        }.forEachIndexed{i,b->b.text=changed[i]}
        back()
    }
    private fun collectionImages():List<String> {
        val prefs=getSharedPreferences("folders",MODE_PRIVATE)
        val folders=(1..2).mapNotNull { prefs.getString("folder_$it",null) }
        val all=mutableListOf<String>()
        for(folder in folders) {
            try {
                val treeUri=android.net.Uri.parse(folder)
                val id=android.provider.DocumentsContract.getTreeDocumentId(treeUri)
                val children=android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(treeUri,id)
                contentResolver.query(children,arrayOf(
                    android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE
                ),null,null,null)?.use { cursor ->
                    while(cursor.moveToNext()) {
                        if((cursor.getString(1) ?: "").startsWith("image/")) {
                            all.add(android.provider.DocumentsContract.buildDocumentUriUsingTree(treeUri,cursor.getString(0)).toString())
                        }
                    }
                }
            } catch (_:Exception) {}
        }
        return all
    }
    private fun reveal() {
        screen("Открытие изображения")
        val page=root
        label("Подбираем фотографию из коллекций…")
        back()
        Thread {
            val uri=ImageLibrary.randomUri(this)
            val bitmap=if(uri!=null) try {
                val source=android.graphics.ImageDecoder.createSource(contentResolver,uri)
                android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val width=info.size.width
                    val height=info.size.height
                    val factor=maxOf(1,(maxOf(width,height)+1199)/1200)
                    decoder.setTargetSize(maxOf(1,width/factor),maxOf(1,height/factor))
                }
            } catch (_:Exception) {null} else null
            runOnUiThread {
                if(root!==page) return@runOnUiThread
                if(bitmap==null || uri==null) {
                    label("Не удалось открыть фото. Проверь папки или импортируй ZIP в настройках.")
                    return@runOnUiThread
                }
                val frame=FrameLayout(this)
                root.addView(frame,1,LinearLayout.LayoutParams(-1,dp(380)))
                val picture=ImageView(this).apply {
                    scaleType=ImageView.ScaleType.FIT_CENTER
                    setImageBitmap(bitmap)
                }
                frame.addView(picture,FrameLayout.LayoutParams(-1,-1))
                val cover=android.widget.GridLayout(this).apply {columnCount=4;rowCount=4}
                frame.addView(cover,FrameLayout.LayoutParams(-1,-1))
                var remaining=16
                repeat(16) { index ->
                    val tile=button("✦",{})
                    val params=android.widget.GridLayout.LayoutParams(
                        android.widget.GridLayout.spec(index/4,1f),
                        android.widget.GridLayout.spec(index%4,1f)
                    ).apply {width=0;height=0;setMargins(1,1,1,1)}
                    cover.addView(tile,params)
                    tile.setOnClickListener {
                        tile.visibility=View.INVISIBLE
                        remaining--
                        if(remaining==0) {
                            award()
                            label("Изображение открыто!")
                            getPreferences(MODE_PRIVATE).edit().putBoolean("unlocked_"+uri.toString(),true).apply()
                        }
                    }
                }
            }
        }.start()
    }
    private fun gallery() {
        startActivity(android.content.Intent(this, GalleryActivity::class.java))
    }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { home() }
}
