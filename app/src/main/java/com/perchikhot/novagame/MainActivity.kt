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
    private fun home() {
        screen("НОВАЯ ИГРА")
        label("Очки: $score  •  Версия 0.1.0")
        add("🧩 Пазлы") { puzzle() }
        add("🃏 Память") { memory() }
        add("💎 Три в ряд") { match3() }
        add("🔎 Найди отличия") { differences() }
        add("✨ Открыть плитки") { reveal() }
        add("🖼 Галерея") { gallery() }\n        add("📁 Папки коллекций") { startActivity(android.content.Intent(this, FolderSettingsActivity::class.java)) }
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
                row.addView(b,LinearLayout.LayoutParams(0,76,1f))
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
        label("Найди 3 отличающихся символа между строками.")
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
        val folderPrefs=getSharedPreferences("folders",MODE_PRIVATE)
        val active=folderPrefs.getInt("active",2)
        val tree=folderPrefs.getString("folder_$active",null)
        if(tree!=null) {
            val result=mutableListOf<String>()
            try {
                val treeUri=android.net.Uri.parse(tree)
                val id=android.provider.DocumentsContract.getTreeDocumentId(treeUri)
                val children=android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(treeUri,id)
                contentResolver.query(children,arrayOf(
                    android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE
                ),null,null,null)?.use { cursor->
                    while(cursor.moveToNext()) {
                        val mime=cursor.getString(1) ?: ""
                        if(mime.startsWith("image/")) {
                            result.add(android.provider.DocumentsContract.buildDocumentUriUsingTree(treeUri,cursor.getString(0)).toString())
                        }
                    }
                }
            } catch (_:Exception) {}
            return result
        }
        return getSharedPreferences("gallery",MODE_PRIVATE).getString("uris","")!!
            .split("\n").filter {it.isNotBlank()}
    }
    private fun reveal() {
        screen("Открытие изображения")
        val images=getSharedPreferences("gallery",MODE_PRIVATE).getString("uris","")!!
            .split("\n").filter { it.isNotBlank() }
        if(images.isEmpty()) {
            label("Сначала добавь изображения через Галерею.")
            add("Перейти в галерею") { gallery() }
            back()
            return
        }
        val index=Random.nextInt(images.size)
        val uri=android.net.Uri.parse(images[index])
        label("Открывай плитки. После 16 нажатий изображение появится целиком.")
        val frame=android.widget.FrameLayout(this)
        root.addView(frame,LinearLayout.LayoutParams(-1,850))
        val picture=ImageView(this).apply {
            scaleType=ImageView.ScaleType.FIT_CENTER
            try {setImageURI(uri)} catch (_:Exception) { }
        }
        frame.addView(picture,android.widget.FrameLayout.LayoutParams(-1,-1))
        val cover=android.widget.GridLayout(this).apply {
            columnCount=4;rowCount=4
        }
        frame.addView(cover,android.widget.FrameLayout.LayoutParams(-1,-1))
        var remaining=16
        repeat(16) {
            val tile=button("✦",{})
            val params=android.widget.GridLayout.LayoutParams(
                android.widget.GridLayout.spec(it/4,1f),
                android.widget.GridLayout.spec(it%4,1f)
            ).apply {width=0;height=0;setMargins(1,1,1,1)}
            cover.addView(tile,params)
            tile.setOnClickListener {
                tile.visibility=android.view.View.INVISIBLE
                remaining--
                if(remaining==0) {
                    award()
                    label("Изображение открыто!")
                    getPreferences(MODE_PRIVATE).edit().putBoolean("unlocked_"+uri.toString(),true).apply()
                }
            }
        }
        back()
    }
    private fun gallery() {
        startActivity(android.content.Intent(this, GalleryActivity::class.java))
    }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { home() }
}
