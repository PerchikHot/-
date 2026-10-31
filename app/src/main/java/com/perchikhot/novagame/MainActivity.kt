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
        ImageLibrary.installBundled(this)
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
    private fun memory(pairLimit:Int=6, daily:Boolean=false, perfection:Boolean=false,
        replayFiles:List<java.io.File>?=null, replayValues:List<Int>?=null,
        limited:Boolean=false) {
        screen(when {
            limited -> "ПАМЯТЬ • ЛИМИТ ХОДОВ"
            daily -> "ПАМЯТЬ • ИСПЫТАНИЕ ДНЯ"
            perfection -> "ПАМЯТЬ • БЕЗ ОШИБОК"
            else -> "ПАМЯТЬ • ФОТО-КАРТОЧКИ"
        })
        label("Открывай по две карточки и находи одинаковые изображения.",15)
        val imported=ImageLibrary.imported(this)
        val shop=getSharedPreferences("unlock_shop",MODE_PRIVATE)
        val allowed=imported.filterIndexed {i,_ -> i==0 || shop.getBoolean("image_$i",false)}
        if(allowed.size<2) {
            label("Для фото-пар открой хотя бы 2 изображения в магазине.",17)
            back()
            return
        }
        // A daily seed gives the same cards and positions for the same
        // unlocked library, day and difficulty. No server or account needed.
        val dailyDay=java.time.LocalDate.now().toString()
        val dailySeed=dailyDay.hashCode() xor (pairLimit * 7919)
        // A replay reuses exactly the same unlocked files and positions.
        // It is practice only: never award currency or update records.
        val practice = replayFiles != null && replayValues != null &&
            replayFiles.size == pairLimit && replayFiles.size in 2..6 &&
            replayFiles.distinctBy {it.absolutePath}.size == replayFiles.size &&
            replayFiles.all {file -> allowed.any {it.absolutePath == file.absolutePath}} &&
            replayValues.size == pairLimit*2 &&
            replayValues.all {it in 0 until pairLimit} &&
            (0 until pairLimit).all {index -> replayValues.count {it==index} == 2}
        val chosen=if(practice) replayFiles!!.toList() else
            (if(daily) allowed.sortedBy {it.absolutePath}
                .shuffled(Random(dailySeed)) else allowed.shuffled())
                .take(minOf(pairLimit,allowed.size))
        val dailyPrefs=getSharedPreferences("memory_daily",MODE_PRIVATE)
        val dailyKey="day_${dailyDay}_pairs_${chosen.size}"
        val records=getSharedPreferences("memory_records",MODE_PRIVATE)
        val recordKey="best_mistakes_${chosen.size}"
        val winsKey="wins_${chosen.size}"
        val perfectWinsKey="perfect_wins_${chosen.size}"
        val perfectBonusKey="perfect_${dailyDay}_pairs_${chosen.size}"
        val previousBest=if(records.contains(recordKey)) records.getInt(recordKey,0) else null
        if(limited) {
            label("Лимит: ${chosen.size*2} хода на ${chosen.size} пар. Подсказки и награды отключены.",14)
        } else if(perfection) {
            label("Режим без ошибок: один промах завершает раунд. " +
                "Побед: ${records.getInt(perfectWinsKey,0)} • " +
                (if(dailyPrefs.getBoolean(perfectBonusKey,false))
                    "Бонус за сегодня получен" else "Первый успех сегодня: бонус"),14)
        } else if(daily) {
            label("Испытание ${dailyDay} • ${chosen.size} пары • " +
                (if(dailyPrefs.getBoolean(dailyKey,false)) "Награда уже получена" else "Первая победа без подсказок: двойные очки"),14)
        } else {
            label("Сложность: ${chosen.size} пары • Побед: ${records.getInt(winsKey,0)} • Лучший результат: ${previousBest ?: "—"} ошибок",14)
        }
        add(if(daily) "Обычная игра" else "Испытание дня") {
            memory(pairLimit,!daily,false)
        }
        add(if(perfection) "Обычная игра" else "Без ошибок") {
            memory(pairLimit,false,!perfection)
        }
        add(if(limited) "Обычная игра" else "Лимит ходов") {
            memory(pairLimit,false,false,limited=!limited)
        }
        val difficulties=LinearLayout(this).apply {orientation=LinearLayout.HORIZONTAL}
        for(pairs in listOf(2,4,6).filter {it<=allowed.size}) {
            difficulties.addView(button("$pairs пары") {memory(pairs,daily,perfection,limited=limited)},
                LinearLayout.LayoutParams(0,dp(48),1f))
        }
        root.addView(difficulties)
        val values=if(practice) replayValues!!.toList() else
            (chosen.indices.toList()+chosen.indices.toList())
                .let { if(daily) it.shuffled(Random(dailySeed xor 0x5A17)) else it.shuffled() }
        add("Повторить раскладку (без наград)") {
            memory(chosen.size,daily,perfection,chosen,values,limited)
        }
        if(practice) label("Тренировка: те же карточки и позиции. Монеты, рекорды и ежедневные награды отключены.",14)
        val thumbs=chosen.map {file ->
            android.graphics.BitmapFactory.decodeFile(file.absolutePath,
                android.graphics.BitmapFactory.Options().apply {inSampleSize=8})
        }
        val opened=mutableSetOf<Int>()
        var first=-1
        var locked=false
        var mistakes=0
        var moves=0
        val maxMoves=if(limited) chosen.size*2 else Int.MAX_VALUE
        var streak=0
        var bestStreak=0
        var roundPoints=0
        var completed=false
        var hintsUsed=0
        val progress=TextView(this).apply {
            text="Найдено пар: 0/${chosen.size} • Ошибки: 0" +
                (if(limited) " • Ходы: 0/$maxMoves" else "")
            textSize=15f
            setTextColor(Color.rgb(177,215,255))
            gravity=Gravity.CENTER
        }
        root.addView(progress)
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
            if(!completed && !locked && i !in opened && i!=first) {
                show(i)
                if(first<0) first=i else {
                    val prev=first
                    first=-1
                    moves++
                    if(values[prev]==values[i]) {
                        opened.add(prev);opened.add(i)
                        streak++
                        bestStreak=maxOf(bestStreak,streak)
                        // Same combo score as the Windows preview. It is a
                        // round-only metric, separate from the shop currency.
                        roundPoints += 100 + 25 * (streak - 1)
                        progress.text="Пары: ${opened.size/2}/${chosen.size} • Ошибки: $mistakes • Серия: $streak • Очки: $roundPoints" +
                            (if(limited) " • Ходы: $moves/$maxMoves" else "")
                        if(!completed && opened.size==values.size) {
                            completed=true
                            val wins=records.getInt(winsKey,0).coerceAtLeast(0)
                            val oldBest=if(records.contains(recordKey)) records.getInt(recordKey,0) else null
                            // Assisted completions still award points, but never alter
                            // the unassisted win count or personal best.
                            val firstPerfectWin=!practice && perfection &&
                                !dailyPrefs.getBoolean(perfectBonusKey,false)
                            if(perfection && !practice && !limited) {
                                records.edit().putInt(perfectWinsKey,
                                    (records.getInt(perfectWinsKey,0).coerceAtLeast(0)+1)
                                        .coerceAtMost(1000000)).apply()
                                if(firstPerfectWin) dailyPrefs.edit()
                                    .putBoolean(perfectBonusKey,true).apply()
                            }
                            if(!practice && !limited && hintsUsed==0 && !daily && !perfection) records.edit()
                                .putInt(winsKey,(wins+1).coerceAtMost(1000000))
                                .putInt(recordKey,if(oldBest==null) mistakes else minOf(oldBest,mistakes)).apply()
                            val firstDailyWin=!practice && daily && hintsUsed==0 &&
                                !dailyPrefs.getBoolean(dailyKey,false)
                            if(firstDailyWin) dailyPrefs.edit().putBoolean(dailyKey,true).apply()
                            val reward=if(practice || limited) 0 else if(perfection) {
                                if(firstPerfectWin) chosen.size*15 else 0
                            } else if(daily) {
                                if(firstDailyWin) chosen.size*10 else 0
                            } else if(tactical) maxOf(10,60-mistakes*5) else chosen.size*5
                            if(reward>0) award(reward)
                            progress.text=when {
                                practice -> "Тренировка пройдена! Очки раунда: $roundPoints • без наград"
                                limited -> "Лимит пройден за $moves/$maxMoves ходов! Очки: $roundPoints • без наград"
                                perfection && firstPerfectWin -> "Без ошибок! +$reward очков • Бонус дня"
                                perfection -> "Без ошибок! Повторное прохождение без бонуса"
                                hintsUsed>0 -> "Победа с подсказками! Рекорды и награда дня не изменены"
                                daily && !firstDailyWin -> "Испытание дня повторено • награда уже получена"
                                daily -> "Испытание дня пройдено! +$reward очков"
                                else -> "Победа! Ошибок: $mistakes • Очки: $roundPoints • Рекорд: ${minOf(oldBest ?: mistakes,mistakes)}"
                            }
                            label(if(reward>0) "Все пары найдены! Счёт раунда: $roundPoints • +$reward монет"
                                else "Все пары найдены! Счёт раунда: $roundPoints • Без повторной награды")
                        }
                        if(!completed && limited && moves>=maxMoves) {
                            completed=true
                            progress.text="Лимит ходов исчерпан: $moves/$maxMoves. Попробуй снова."
                            add("Повторить лимит") { memory(pairLimit,limited=true) }
                        }
                    } else {
                        mistakes++
                        streak=0
                        val lost=perfection || (limited && moves>=maxMoves)
                        progress.text=if(lost) "Раунд завершён • ${opened.size/2}/${chosen.size} пар • $moves/$maxMoves ходов"
                            else "Пары: ${opened.size/2}/${chosen.size} • Ошибки: $mistakes • Серия: $streak • Очки: $roundPoints" +
                                (if(limited) " • Ходы: $moves/$maxMoves" else "")
                        if(lost) {
                            completed=true
                            label(if(limited) "Ходы закончились. Начни новое испытание."
                                else "Испытание провалено. Начни новый раунд без ошибок.")
                            add(if(limited) "Повторить лимит" else "Повторить без ошибок") {
                                memory(pairLimit,false,!limited,limited=limited)
                            }
                        }
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
        lateinit var hintButton:Button
        hintButton=button("Подсказка: 2 осталось") {
            if(!perfection && !limited && !completed && !locked && first<0 && hintsUsed<2) {
                val remaining=values.indices.filter {it !in opened}
                var pair:Pair<Int,Int>?=null
                for(i in remaining) {
                    val other=remaining.firstOrNull {it!=i && values[it]==values[i]}
                    if(other!=null) {pair=i to other;break}
                }
                pair?.let {(a,b) ->
                    hintsUsed++
                    locked=true
                    show(a);show(b)
                    hintButton.text="Подсказок осталось: ${2-hintsUsed}"
                    progress.text="Подсказка ${hintsUsed}/2 • Очки: $roundPoints • Рекорд без подсказок"
                    hintButton.postDelayed({
                        if(!isFinishing && !isDestroyed) {
                            if(a !in opened) hide(a)
                            if(b !in opened) hide(b)
                            locked=false
                        }
                    },1400)
                }
            }
        }
        if(perfection || limited) {
            hintButton.text="Подсказки отключены в этом режиме"
            hintButton.isEnabled=false
        }
        root.addView(hintButton)
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
                var moves=0
                var rewarded=false
                val records=getSharedPreferences("puzzle_records",MODE_PRIVATE)
                val previousBest=records.getInt("best_moves_3x3",0).takeIf {it>0}
                val progressLabel=TextView(this).apply {
                    textSize=17f
                    setTextColor(Color.rgb(177,215,255))
                    gravity=Gravity.CENTER
                    text="Ходы: 0 • Рекорд: ${previousBest ?: "—"}"
                }
                root.addView(progressLabel,1)
                repeat(90) {
                    val z=cells.indexOf(8)
                    val neighbors=listOf(z-3,z+3,if(z%3>0)z-1 else -1,if(z%3<2)z+1 else -1).filter {it in 0..8}
                    val n=neighbors.random()
                    val t=cells[n];cells[n]=8;cells[z]=t
                }
                // A random walk can return to the solved board. Start with a
                // real puzzle, never a zero-move reward.
                if(cells==(0..8).toList()) {
                    val z=cells.indexOf(8)
                    val n=z-1
                    cells[z]=cells[n];cells[n]=8
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
                                moves++
                                draw()
                                progressLabel.text="Ходы: $moves • Рекорд: ${records.getInt("best_moves_3x3",0).takeIf {it>0} ?: "—"}"
                                if(!rewarded && cells==(0..8).toList()) {
                                    rewarded=true
                                    val best=records.getInt("best_moves_3x3",0)
                                    if(best<=0 || moves<best) {
                                        records.edit().putInt("best_moves_3x3",moves).apply()
                                    }
                                    progressLabel.text="Собрано за $moves ходов • Рекорд: ${records.getInt("best_moves_3x3",moves)}"
                                    award(30)
                                    label("Пазл собран! +30 очков. Для новой попытки нажми «Начать заново».")
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
    private fun characterCompanion(): (String)->Unit {
        val imported=ImageLibrary.imported(this)
        val unlocked=getSharedPreferences("unlock_shop",MODE_PRIVATE)
        val groupPrefs=getSharedPreferences("image_groups",MODE_PRIVATE)
        fun groupName(file:java.io.File)=groupPrefs.getString(file.name,file.nameWithoutExtension) ?: file.nameWithoutExtension
        val available=imported.filterIndexed {i,file -> file.name.startsWith("bundled_") || i==0 || unlocked.getBoolean("image_$i",false)}
        val grouped=available.groupBy {groupName(it)}
        val selected=grouped.keys.randomOrNull() ?: ChibiArt.roster.random()
        val photos=grouped[selected].orEmpty()
        val row=LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL
            gravity=Gravity.RIGHT or Gravity.CENTER_VERTICAL
            setPadding(dp(6),dp(4),dp(6),dp(8))
        }
        val message=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER}
        val name=TextView(this).apply {
            text=mapOf("Aqua" to "Аква","Shinobu" to "Синобу","Erza" to "Эрза","Temari" to "Темари","Tenten" to "Тен-Тен","Nezuko" to "Незуко","ZeroTwo" to "Зеро Ту","Rias" to "Риас","Mitsuri" to "Мицури","Nami" to "Нами","Alpha" to "Альфа","Yamato" to "Ямато","Shion" to "Шион","Noelle" to "Ноэль","Alice" to "Алиса","Hinata" to "Хината","Rebecca" to "Ребекка","Asuka" to "Аска","Orihime" to "Орихиме","Rukia" to "Рукия","Hancock" to "Хэнкок","Nobara" to "Нобара","Mimosa" to "Мимоза","Shuna" to "Шуна","Darkness" to "Даркнесс","Yuno" to "Юно").getOrDefault(selected,selected ?: "Героиня")
            textSize=12f;setTextColor(0xFFE5B7FF.toInt());gravity=Gravity.CENTER
        }
        message.addView(name)
        val speech=TextView(this).apply {
            text="Давай сыграем! ✨";textSize=14f;setTextColor(Color.WHITE)
            gravity=Gravity.CENTER;background=panel(0xFF34305F.toInt(),14)
            setPadding(dp(8),dp(8),dp(8),dp(8))
        }
        message.addView(speech)
        row.addView(message,LinearLayout.LayoutParams(0,-2,1f).apply {rightMargin=dp(8)})
        val pictureFrame=FrameLayout(this)
        val portrait=ImageView(this).apply {
            scaleType=ImageView.ScaleType.FIT_CENTER
            background=panel(0xFF3D316A.toInt(),16)
            contentDescription=selected ?: "Героиня"
        }
        pictureFrame.addView(portrait,FrameLayout.LayoutParams(-1,-1))
        val mood=TextView(this).apply {
            text="😊";textSize=22f;gravity=Gravity.CENTER
            background=panel(0xCC292149.toInt(),14)
        }
        pictureFrame.addView(mood,FrameLayout.LayoutParams(dp(37),dp(37),Gravity.TOP or Gravity.RIGHT))
        row.addView(pictureFrame,LinearLayout.LayoutParams(dp(128),dp(158)))
        root.addView(row,1)
        fun showPhoto(index:Int) {
            val file=photos.getOrNull(index)
            if(file==null) {portrait.setImageResource(android.R.drawable.ic_menu_gallery);return}
            val bitmap=android.graphics.BitmapFactory.decodeFile(file.absolutePath,
                android.graphics.BitmapFactory.Options().apply {inSampleSize=8})
            if(bitmap!=null) portrait.setImageBitmap(bitmap)
        }
        // Each character can ship chibi/CHARACTER/{idle,happy,excited,surprised,victory}.png
        val characterKey=(photos.firstOrNull()?.nameWithoutExtension?.removePrefix("bundled_")?.substringBefore("__") ?: (selected ?: "")).replace(Regex("[^A-Za-z0-9_-]"),"_")
        fun showMood(kind:String) {
            val moodFile=when(kind) {
                "great" -> "excited"
                "good" -> "happy"
                "miss" -> "surprised"
                "win" -> "victory"
                else -> "idle"
            }
            val paths=listOf("chibi/$characterKey/$moodFile.png","chibi/$characterKey/idle.png")
            var sprite:android.graphics.Bitmap?=null
            for(asset in paths) {
                try {sprite=assets.open(asset).use {android.graphics.BitmapFactory.decodeStream(it)}} catch (_:Exception) {}
                if(sprite!=null) break
            }
            if(sprite!=null) portrait.setImageBitmap(sprite) else portrait.setImageBitmap(ChibiArt.render(characterKey,moodFile))
        }
        showMood("idle")
        var reactionId=0
        return { kind:String ->
            reactionId++
            val id=reactionId
            speech.text=when(kind) {
                "great" -> listOf("Вот это комбо! ✨","Невероятно! 🌟","Ты молодец! 💜").random()
                "good" -> listOf("Отличный ход!","Так держать! 💜","Здорово получилось!").random()
                "miss" -> listOf("Попробуй иначе!","Почти получилось!","Не сдавайся!").random()
                "win" -> "Ура! Победа! 🏆"
                else -> "Я рядом! ✨"
            }
            mood.text=when(kind) {
                "great" -> "😍"
                "good" -> "🥰"
                "miss" -> "😯"
                "win" -> "🥳"
                else -> "😊"
            }
            portrait.animate().alpha(0.3f).setDuration(120).withEndAction {
                showMood(kind)
                portrait.animate().alpha(1f).setDuration(220).start()
            }.start()
            pictureFrame.animate().cancel()
            pictureFrame.animate().scaleX(if(kind=="great" || kind=="win") 1.16f else 1.07f)
                .scaleY(if(kind=="great" || kind=="win") 1.16f else 1.07f)
                .setDuration(200).withEndAction {
                    pictureFrame.animate().scaleX(1f).scaleY(1f).setDuration(260).start()
                }.start()
            row.postDelayed({
                if(reactionId==id && kind!="win") {
                    speech.text="Твой ход! ✨"
                    mood.text="😊"
                    showMood("idle")
                }
            },2400)
        }
    }
    private fun match3() {
        screen("Три в ряд")
        val react=characterCompanion()
        label("Меняй соседние камни местами. 3 в ряд — исчезновение, 4+ — бонус, 5+ — взрыв соседних клеток!",15)
        val tiles=MutableList(36){Random.nextInt(5)}
        val icons=listOf("💎","🔮","💠","🌟","❤️")
        var selected=-1
        var busy=false
        var moves=0
        var chain=0
        var charges=0
        var cleared=0
        var completed=false
        val arcade=intent.getStringExtra("prototype")=="arcade"
        val maxMoves=if(arcade) 16 else Int.MAX_VALUE
        val goal=if(arcade) 30 else 100
        val blocked=if(arcade) setOf(7,28) else emptySet<Int>()
        val status=TextView(this).apply {text=if(arcade) "АРКАДА • 16 ходов • Цель: 30" else "∞ БЕСКОНЕЧНЫЙ РЕЖИМ • Собрано: 0 • Рекорд: ${getSharedPreferences("endless_match3",MODE_PRIVATE).getInt("best",0)}";textSize=17f;setTextColor(Color.WHITE);gravity=Gravity.CENTER}
        if(arcade) label("⚡ Комбинации из 4+ заряжают удар по ряду. Заряды: до 3. Используй усилитель в нужный момент!",14)
        root.addView(status)
        val progress=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply {
            max=goal
            progress=0
            progressTintList=android.content.res.ColorStateList.valueOf(0xFFB86DFF.toInt())
        }
        root.addView(progress,LinearLayout.LayoutParams(-1,dp(12)).apply {bottomMargin=dp(12)})
        lateinit var buttons:List<Button>
        var powerButton:Button?=null
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
            powerButton?.apply {
                text="⚡ УДАР ПО РЯДУ • $charges/3"
                isEnabled=charges>0 && !busy && !completed
            }
            buttons.forEachIndexed { i,b ->
                b.text=if(i in blocked) "✕" else icons[tiles[i]]
                b.isEnabled=i !in blocked
                b.alpha=if(i==selected) .6f else 1f
                b.background=panel(if(i==selected) 0xFFAC66F0.toInt() else listOf(0xFF214A91,0xFF633CA5,0xFF226C89,0xFF8B5D26,0xFF8D345C)[tiles[i]].toInt(),14)
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
            react(if(found.size>=5 || chain>=3) "great" else "good")
            if(bonus>0) {
                charges=minOf(3,charges+1)
                cleared+=bonus
                powerButton?.text="⚡ УДАР ПО РЯДУ • $charges/3"
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
                progress.progress=if(arcade) minOf(goal,cleared) else cleared%goal
                if(arcade && cleared>=goal && !completed) {
                    completed=true
                    status.text="ПОБЕДА! Собрано $cleared элементов"
                    react("win")
                    award(if(arcade) 75 else 40)
                }
                if(!completed) {
                    if(!arcade) {
                        val prefs=getSharedPreferences("endless_match3",MODE_PRIVATE)
                        if(cleared>prefs.getInt("best",0)) prefs.edit().putInt("best",cleared).apply()
                        if(cleared/goal>(cleared-found.size)/goal) { react("win"); award(40) }
                        status.text="∞ БЕСКОНЕЧНО • Ходы: $moves • Собрано: $cleared • Рекорд: ${prefs.getInt("best",0)} • КОМБО x$chain"
                    } else status.text="Ходы: $moves / $maxMoves • Собрано: $cleared / $goal • КОМБО x$chain"
                }
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
                            react("miss")
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
        if(arcade) {
            val power=button("⚡ УДАР ПО РЯДУ • 0/3") {}
            powerButton=power
            root.addView(power)
            fun refreshPower() {
                power.text="⚡ УДАР ПО РЯДУ • $charges/3"
                power.isEnabled=charges>0 && !busy && !completed
            }
            power.setOnClickListener {
                if(charges>0 && !busy && !completed) {
                    charges--
                    busy=true
                    val row=(0..5).filter { r -> (r*6 until r*6+6).all {it !in blocked} }.randomOrNull() ?: 0
                    val hit=(row*6 until row*6+6).filter {it !in blocked}.toSet()
                    hit.forEach {buttons[it].animate().alpha(.2f).setDuration(200).start()}
                    power.postDelayed({
                        gravityFill(hit)
                        cleared+=hit.size
                        progress.progress=minOf(goal,cleared)
                        buttons.forEach {it.alpha=1f}
                        status.text="⚡ ОЧИЩЕН РЯД ${row+1} • +${hit.size}"
                        if(cleared>=goal && !completed) {
                            completed=true
                            react("win")
                            award(75)
                        }
                        render()
                        if(!completed) settle() else busy=false
                        refreshPower()
                    },230)
                }
            }
            power.postDelayed({refreshPower()},100)
        }
        // Drag a gem toward an adjacent cell to swap it, as in mobile match-3 games.
        buttons.forEachIndexed { index, gem ->
            var downX=0f
            var downY=0f
            gem.setOnTouchListener { view, event ->
                when(event.actionMasked) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        downX=event.x; downY=event.y
                        true
                    }
                    android.view.MotionEvent.ACTION_UP -> {
                        val dx=event.x-downX
                        val dy=event.y-downY
                        val threshold=dp(18)
                        if(kotlin.math.max(kotlin.math.abs(dx),kotlin.math.abs(dy))<threshold) {
                            view.performClick()
                        } else {
                            val target=if(kotlin.math.abs(dx)>kotlin.math.abs(dy)) {
                                if(dx>0 && index%6<5) index+1 else if(dx<0 && index%6>0) index-1 else -1
                            } else {
                                if(dy>0 && index/6<5) index+6 else if(dy<0 && index/6>0) index-6 else -1
                            }
                            if(target in 0..35 && index !in blocked && target !in blocked && !busy && !completed) {
                                selected=index
                                buttons[target].performClick()
                            }
                        }
                        true
                    }
                    else -> true
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
