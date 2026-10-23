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
    private var journeyAwarded = false
    private val bg = Color.rgb(9, 17, 39)
    private val blue = Color.rgb(41, 101, 201)
    private val symbols = listOf("★","◆","♥","☀","♣","✿")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        score = getPreferences(MODE_PRIVATE).getInt("score",0)
        if (ImageLibrary.imported(this).isEmpty() && (1..2).all { getSharedPreferences("folders",MODE_PRIVATE).getString("folder_$it",null)==null }) {
            startActivity(android.content.Intent(this,FolderSettingsActivity::class.java))
        }
        when(intent.getStringExtra("mode")) {
            "puzzle" -> puzzle()
            "memory" -> memory()
            "match3" -> match3()
            "differences" -> differences()
            "reveal" -> reveal()
            else -> home()
        }
    }
    private fun screen(title: String) {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(18), dp(14), dp(18))
        }
        val frame=FrameLayout(this).apply {setBackgroundColor(bg)}
        val photo=ImageView(this).apply {scaleType=ImageView.ScaleType.CENTER_CROP;alpha=.30f}
        frame.addView(photo,FrameLayout.LayoutParams(-1,-1))
        val scroll=ScrollView(this)
        scroll.addView(root)
        frame.addView(scroll,FrameLayout.LayoutParams(-1,-1))
        setContentView(frame)
        val files=ImageLibrary.imported(this)
        val shop=getSharedPreferences("unlock_shop",MODE_PRIVATE)
        val available=files.filterIndexed {i,_ -> i==0 || shop.getBoolean("image_$i",false)}
        if(available.isNotEmpty()) Thread {
            val bmp=android.graphics.BitmapFactory.decodeFile(available.random().absolutePath,
                android.graphics.BitmapFactory.Options().apply {inSampleSize=2})
            runOnUiThread {if(!isFinishing) photo.setImageBitmap(bmp)}
        }.start()
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
            background=panel(0xD0272855.toInt(),14)
            setOnClickListener { action() }
        }
    }
    private fun add(s:String, action:()->Unit) { root.addView(button(s,action)) }
    private fun award(points:Int=10) {
        score+=points
        if(intent.getBooleanExtra("journey",false) && !journeyAwarded) {
            journeyAwarded=true
            val progress=getSharedPreferences("progress",MODE_PRIVATE)
            progress.edit().putInt("wins",progress.getInt("wins",0)+1).apply()
            android.app.AlertDialog.Builder(this)
                .setTitle("✦ УРОВЕНЬ ПРОЙДЕН!")
                .setMessage("Новый этап открыт! Награда: +$points очков.\nПродолжай путь и открывай изображения в магазине.")
                .setPositiveButton("К СЛЕДУЮЩЕМУ УРОВНЮ") { _,_ -> finish() }
                .setNegativeButton("ОСТАТЬСЯ",null)
                .show()
        }
        getPreferences(MODE_PRIVATE).edit().putInt("score",score).apply()
        Toast.makeText(this,"+$points очков! Всего: $score",Toast.LENGTH_SHORT).show()
    }
    private fun back() {
        root.addView(button("↻ НАЧАТЬ ЗАНОВО") {
            val restart=android.content.Intent(this,MainActivity::class.java).apply {putExtras(intent)}
            startActivity(restart)
            finish()
        },LinearLayout.LayoutParams(-1,dp(52)).apply {topMargin=dp(10)})
        root.addView(button("← ВЫБОР РЕЖИМОВ") {finish()},
            LinearLayout.LayoutParams(-1,dp(52)).apply {topMargin=dp(14)})
    }
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
                row.addView(b,LinearLayout.LayoutParams(0,dp(if(cols==6) 53 else 78),1f).apply {setMargins(dp(2),dp(3),dp(2),dp(3))})
                b.minHeight=0; b.setPadding(0,0,0,0); b.textSize=20f
                b.setOnClickListener { onCell(index,b) }
                buttons.add(b)
            }
        }
        return buttons
    }
    private fun memory() {
        screen("ПАМЯТЬ • ФОТО-КАРТОЧКИ")
        label("Открывай по две карточки и находи одинаковые изображения.",15)
        val imported=ImageLibrary.imported(this)
        val shop=getSharedPreferences("unlock_shop",MODE_PRIVATE)
        val allowed=imported.filterIndexed {i,_ -> i==0 || shop.getBoolean("image_$i",false)}
        if(allowed.size<2) {
            label("Для фото-пар открой хотя бы 2 изображения в магазине.",17)
            back()
            return
        }
        val chosen=allowed.shuffled().take(minOf(6,allowed.size))
        val values=(chosen.indices.toList()+chosen.indices.toList()).shuffled()
        val thumbs=chosen.map {file ->
            android.graphics.BitmapFactory.decodeFile(file.absolutePath,
                android.graphics.BitmapFactory.Options().apply {inSampleSize=8})
        }
        val opened=mutableSetOf<Int>()
        var first=-1
        var locked=false
        var mistakes=0
        val tactical=intent.getStringExtra("prototype")=="tactics"
        lateinit var buttons:List<Button>
        fun show(i:Int) {
            buttons[i].text=""
            buttons[i].background=android.graphics.drawable.BitmapDrawable(resources,thumbs[values[i]])
        }
        fun hide(i:Int) {
            buttons[i].text="✧"
            buttons[i].background=panel(0xD032285B.toInt(),14)
        }
        buttons=grid(3,values.size) {i,_ ->
            if(!locked && i !in opened && i!=first) {
                show(i)
                if(first<0) first=i else {
                    val prev=first
                    first=-1
                    if(values[prev]==values[i]) {
                        opened.add(prev);opened.add(i)
                        if(opened.size==values.size) {
                            award(if(tactical) maxOf(10,60-mistakes*5) else 30)
                            label("Все пары найдены! +30 очков")
                        }
                    } else {
                        mistakes++
                        if(tactical) Toast.makeText(this,"Ошибки: $mistakes • Бонус уменьшается",Toast.LENGTH_SHORT).show()
                        locked=true
                        buttons[i].postDelayed({
                            if(!isFinishing) {hide(prev);hide(i)}
                            locked=false
                        },850)
                    }
                }
            }
        }
        buttons.forEachIndexed {i,_->hide(i)}
        back()
    }
    private fun puzzle() {
        screen("Фото-пазл")
        val page=root
        label("Загружаем случайную фотографию…",16)
        back()
        Thread {
            val uri=ImageLibrary.randomUri(this)
            val bitmap=if(uri!=null) try {
                val source=android.graphics.ImageDecoder.createSource(contentResolver,uri)
                android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val size=minOf(info.size.width,info.size.height)
                    decoder.setTargetSize(600,600)
                }
            } catch (_:Exception) {null} else null
            runOnUiThread {
                if(root!==page) return@runOnUiThread
                if(bitmap==null) {
                    label("Нет доступных фото. Импортируй ZIP через «Мои папки».")
                    return@runOnUiThread
                }
                label("Передвигай фрагменты рядом с пустой клеткой, чтобы собрать картинку.",15)
                val side=minOf(bitmap.width,bitmap.height)
                val square=android.graphics.Bitmap.createBitmap(bitmap,(bitmap.width-side)/2,(bitmap.height-side)/2,side,side)
                val tileSize=side/3
                val parts=(0..8).map { i ->
                    android.graphics.Bitmap.createBitmap(square,(i%3)*tileSize,(i/3)*tileSize,tileSize,tileSize)
                }
                val cells=(0..8).toMutableList()
                repeat(90) {
                    val z=cells.indexOf(8)
                    val neighbors=listOf(z-3,z+3,if(z%3>0)z-1 else -1,if(z%3<2)z+1 else -1).filter {it in 0..8}
                    val n=neighbors.random()
                    val t=cells[n];cells[n]=8;cells[z]=t
                }
                val board=android.widget.GridLayout(this).apply {columnCount=3;rowCount=3}
                root.addView(board,1,LinearLayout.LayoutParams(-1,dp(360)))
                val views=mutableListOf<ImageView>()
                fun draw() {
                    views.forEachIndexed { i,view ->
                        if(cells[i]==8) {
                            view.setImageDrawable(null)
                            view.setBackgroundColor(Color.rgb(46,33,79))
                        } else {
                            view.setImageBitmap(parts[cells[i]])
                            view.setBackgroundColor(Color.rgb(20,22,45))
                        }
                    }
                }
                repeat(9) {i ->
                    val view=ImageView(this).apply {
                        scaleType=ImageView.ScaleType.FIT_XY
                        setOnClickListener {
                            val z=cells.indexOf(8)
                            if((i/3==z/3 && kotlin.math.abs(i-z)==1)||kotlin.math.abs(i-z)==3) {
                                cells[z]=cells[i];cells[i]=8
                                draw()
                                if(cells==(0..8).toList()) {
                                    award(30)
                                    label("Пазл собран! +30 очков")
                                }
                            }
                        }
                    }
                    val params=android.widget.GridLayout.LayoutParams(
                        android.widget.GridLayout.spec(i/3,1f),
                        android.widget.GridLayout.spec(i%3,1f)
                    ).apply {width=0;height=0;setMargins(dp(2),dp(2),dp(2),dp(2))}
                    board.addView(view,params)
                    views.add(view)
                }
                draw()
                val preview=ImageView(this).apply {setImageBitmap(square);scaleType=ImageView.ScaleType.FIT_CENTER}
                label("Образец",16)
                root.addView(preview,LinearLayout.LayoutParams(-1,dp(150)))
            }
        }.start()
    }
    private fun match3() {
        screen("Три в ряд")
        label("Выбери камень, затем соседний. Три одинаковых по горизонтали или вертикали исчезнут. Новые упадут сверху.",15)
        val tiles=MutableList(36){Random.nextInt(5)}
        val icons=listOf("◆","✦","●","⬟","✿")
        var selected=-1
        var busy=false
        var moves=0
        var chain=0
        var cleared=0
        var completed=false
        val arcade=intent.getStringExtra("prototype")=="arcade"
        val maxMoves=if(arcade) 16 else Int.MAX_VALUE
        val goal=if(arcade) 30 else 35
        val blocked=if(arcade) setOf(7,28) else emptySet<Int>()
        val status=TextView(this).apply {text=if(arcade) "АРКАДА • 16 ходов • Цель: 30" else "Собери 35 элементов";textSize=17f;setTextColor(Color.WHITE);gravity=Gravity.CENTER}
        root.addView(status)
        val progress=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply {
            max=goal
            progress=0
            progressTintList=android.content.res.ColorStateList.valueOf(0xFFB86DFF.toInt())
        }
        root.addView(progress,LinearLayout.LayoutParams(-1,dp(12)).apply {bottomMargin=dp(12)})
        lateinit var buttons:List<Button>
        fun matches():Set<Int> {
            val found=mutableSetOf<Int>()
            for(row in 0..5) for(col in 0..5) {
                val i=row*6+col
                if(col<4 && i !in blocked && i+1 !in blocked && i+2 !in blocked && tiles[i]==tiles[i+1] && tiles[i]==tiles[i+2]) found.addAll(listOf(i,i+1,i+2))
                if(row<4 && i !in blocked && i+6 !in blocked && i+12 !in blocked && tiles[i]==tiles[i+6] && tiles[i]==tiles[i+12]) found.addAll(listOf(i,i+6,i+12))
            }
            return found
        }
        fun render() {
            buttons.forEachIndexed { i,b ->
                b.text=if(i in blocked) "✕" else icons[tiles[i]]
                b.isEnabled=i !in blocked
                b.alpha=if(i==selected) .6f else 1f
                b.background=panel(if(i==selected) 0xF08D52B6.toInt() else 0xD0252A5A.toInt(),12)
            }
        }
        fun gravityFill(found:Set<Int>) {
            for(col in 0..5) {
                var segment=mutableListOf<Int>()
                fun refillSegment() {
                    if(segment.isEmpty()) return
                    val survivors=segment.asReversed().filter { it !in found }.map { tiles[it] }
                    segment.asReversed().forEachIndexed { index,position ->
                        tiles[position]=if(index<survivors.size) survivors[index] else Random.nextInt(5)
                    }
                    segment=mutableListOf()
                }
                for(row in 5 downTo 0) {
                    val position=row*6+col
                    if(position in blocked) refillSegment() else segment.add(position)
                }
                refillSegment()
            }
        }
        fun settle() {
            val matched=matches()
            val found=matched.toMutableSet()
            if(matched.size>=5) {
                matched.forEach { index ->
                    val row=index/6;val col=index%6
                    for(next in listOf(index-6,index+6,index-1,index+1)) {
                        if(next in 0..35 && next !in blocked && kotlin.math.abs(next/6-row)+kotlin.math.abs(next%6-col)==1) found.add(next)
                    }
                }
            }
            if(found.isEmpty()) {
                chain=0
                busy=false
                render()
                if(arcade && !completed && moves<maxMoves) {
                    var hasMove=false
                    for(i in tiles.indices) {
                        if(i in blocked) continue
                        for(j in listOf(i+1,i+6)) {
                            if(j>=tiles.size || j in blocked || (j==i+1 && i/6!=j/6)) continue
                            val t=tiles[i];tiles[i]=tiles[j];tiles[j]=t
                            if(matches().isNotEmpty()) hasMove=true
                            tiles[j]=tiles[i];tiles[i]=t
                        }
                    }
                    if(!hasMove && root.findViewWithTag<View>("arcade_stuck")==null) {
                        status.text="Ходов больше нет — начни новое поле"
                        root.addView(button("↻ НОВОЕ ПОЛЕ") {match3()}.apply {tag="arcade_stuck"})
                    }
                }
                if(arcade && moves>=maxMoves && !completed && root.findViewWithTag<View>("arcade_retry")==null) {
                    status.text="ПОРАЖЕНИЕ • Собрано $cleared из $goal"
                    root.addView(button("↻ ЕЩЁ ПОПЫТКА") {match3()}.apply {tag="arcade_retry"})
                }
                return
            }
            chain++
            val bonus=if(found.size>=5) 5 else if(found.size>=4) 2 else 0
            if(bonus>0) {
                cleared+=bonus
                status.text=if(bonus==5) "🌟 СУПЕРКОМБО! +5 к прогрессу" else "⚡ БОНУС! +2 к прогрессу"
            }
            found.forEach { i ->
                buttons[i].text="✦"
                buttons[i].animate().scaleX(1.22f).scaleY(1.22f).alpha(.2f).setDuration(220).start()
            }
            buttons[0].postDelayed({
                gravityFill(found)
                buttons.forEach {it.alpha=1f;it.scaleX=1f;it.scaleY=1f}
                cleared+=found.size
                progress.progress=minOf(goal,cleared)
                if(cleared>=goal && !completed) {
                    completed=true
                    status.text="ПОБЕДА! Собрано $cleared элементов"
                    award(if(arcade) 75 else 40)
                }
                if(!completed) status.text="Ходы: $moves / ${if(arcade) maxMoves else "∞"} • Собрано: $cleared / $goal • КОМБО x$chain"
                render()
                buttons[0].postDelayed({settle()},220)
            },260)
        }
        buttons=grid(6,36) {i,_ ->
            if(i !in blocked && !busy && !completed && moves<maxMoves) {
                if(selected<0) {selected=i;render()}
                else {
                    val previous=selected;selected=-1
                    if(previous==i) render()
                    else if((previous/6==i/6 && kotlin.math.abs(previous-i)==1)||kotlin.math.abs(previous-i)==6) {
                        val t=tiles[i];tiles[i]=tiles[previous];tiles[previous]=t
                        if(matches().isEmpty()) {
                            tiles[previous]=tiles[i];tiles[i]=t
                            status.text="Нет комбинации — попробуй другой ход"
                            render()
                        } else {
                            moves++
                            busy=true
                            status.text="Ходы: $moves • Комбинация!"
                            render()
                            settle()
                        }
                    } else {selected=i;render()}
                }
            }
        }
        // Clear initial matches before the player starts.
        var initialMatches=matches()
        var tries=0
        while(initialMatches.isNotEmpty() && tries<100) {
            initialMatches.forEach {tiles[it]=Random.nextInt(5)}
            initialMatches=matches()
            tries++
        }
        render()
        if(arcade) {
            var possible=false
            for(i in tiles.indices) {
                if(i in blocked) continue
                for(j in listOf(i+1,i+6)) {
                    if(j>=tiles.size || j in blocked || (j==i+1 && i/6!=j/6)) continue
                    val t=tiles[i];tiles[i]=tiles[j];tiles[j]=t
                    val valid=matches().isNotEmpty()
                    tiles[j]=tiles[i];tiles[i]=t
                    if(valid) possible=true
                }
            }
            if(!possible) {
                var retries=0
                while(!possible && retries<150) {
                    for(i in tiles.indices) if(i !in blocked) tiles[i]=Random.nextInt(5)
                    val immediate=matches()
                    if(immediate.isEmpty()) {
                        for(i in tiles.indices) {
                            if(i in blocked) continue
                            for(j in listOf(i+1,i+6)) {
                                if(j>=tiles.size || j in blocked || (j==i+1 && i/6!=j/6)) continue
                                val t=tiles[i];tiles[i]=tiles[j];tiles[j]=t
                                if(matches().isNotEmpty()) possible=true
                                tiles[j]=tiles[i];tiles[i]=t
                            }
                        }
                    }
                    retries++
                }
                render()
                if(!possible) {
                    root.addView(button("↻ НОВОЕ ПОЛЕ") {match3()})
                    status.text="Не удалось создать игровое поле"
                }
            }
        }
        if(arcade) {
            var hintsUsed=0
            root.addView(button("✦ ПОДСКАЗКА (2)") {
                if(!busy && !completed && hintsUsed<2) {
                    var suggested:Pair<Int,Int>?=null
                    for(i in tiles.indices) {
                        if(i in blocked) continue
                        for(j in listOf(i+1,i+6)) {
                            if(j>=tiles.size || j in blocked || (j==i+1 && i/6!=j/6)) continue
                            val original=tiles[i]
                            tiles[i]=tiles[j]
                            tiles[j]=original
                            val valid=matches().isNotEmpty()
                            tiles[j]=tiles[i]
                            tiles[i]=original
                            if(valid) {suggested=i to j;break}
                        }
                        if(suggested!=null) break
                    }
                    if(suggested!=null) {
                        hintsUsed++
                        selected=suggested.first
                        render()
                        status.text="Подсказка: клетки ${suggested.first+1} и ${suggested.second+1} • Осталось ${2-hintsUsed}"
                    } else status.text="Доступных комбинаций нет — начни заново"
                }
            })
        }
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
                var moves=0
                label("Найди 3 звезды под плитками за 9 попыток!",15)
                val stars=(0 until 16).shuffled().take(3).toSet()
                val foundStars=mutableSetOf<Int>()
                repeat(16) { index ->
                    val tile=button("",{}).apply {background=panel(if((index/4+index%4)%2==0) 0xE03C3167.toInt() else 0xE023254F.toInt(),5)}
                    val params=android.widget.GridLayout.LayoutParams(
                        android.widget.GridLayout.spec(index/4,1f),
                        android.widget.GridLayout.spec(index%4,1f)
                    ).apply {width=0;height=0;setMargins(1,1,1,1)}
                    cover.addView(tile,params)
                    tile.setOnClickListener {
                        if(!tile.isEnabled || moves>=9) return@setOnClickListener
                        tile.isEnabled=false
                        if(index in stars) foundStars.add(index)
                        tile.text=if(index in stars) "★" else "·"
                        Toast.makeText(this,if(index in stars) "★ Звезда найдена! ${foundStars.size}/3" else "Пусто • Осталось ${9-moves-1} попыток",Toast.LENGTH_SHORT).show()
                        tile.animate().alpha(0f).scaleX(.8f).scaleY(.8f).setDuration(200).withEndAction {tile.visibility=View.INVISIBLE}.start()
                        moves++
                        remaining--
                        if(foundStars.size==3) {
                            award(30+(9-moves)*5)
                            label("Три звезды найдены! Изображение открыто!")
                            getPreferences(MODE_PRIVATE).edit().putBoolean("unlocked_"+uri.toString(),true).apply()
                            for(j in 0 until cover.childCount) cover.getChildAt(j).isEnabled=false
                        } else if(moves==9) {
                            label("Попытки закончились. Найдено звёзд: ${foundStars.size}/3")
                            root.addView(button("↻ ПОВТОРИТЬ") {reveal()})
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
    override fun onBackPressed() { finish() }
}
