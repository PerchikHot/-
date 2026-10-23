package com.perchikhot.novagame

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/** Built-in, offline, procedural chibi placeholders. Replaced by per-character PNGs when supplied. */
object ChibiArt {
    val roster=listOf("Aqua","Erza","Temari","Tenten","Akira","Shion","Darkness","Yuno","Nami","BlueSwordswoman","Rukia","Rebecca","Orihime","Noelle","Asuka","Alice","Hinata","Hancock","Nezuko","ZeroTwo","Nobara","Mitsuri","Yamato","Esil","Shinobu","Mimosa","Alpha","Rias","Shuna","Luna")
    private val hair=mapOf(
        "Aqua" to android.graphics.Color.parseColor("#73C5FF"),
        "Erza" to android.graphics.Color.parseColor("#C92D47"),
        "Temari" to android.graphics.Color.parseColor("#E2B75C"),
        "Tenten" to android.graphics.Color.parseColor("#272A39"),
        "Akira" to android.graphics.Color.parseColor("#54334C"),
        "Shion" to android.graphics.Color.parseColor("#C8A1EA"),
        "Darkness" to android.graphics.Color.parseColor("#F7CB70"),
        "Yuno" to android.graphics.Color.parseColor("#FF85B2"),
        "Nami" to android.graphics.Color.parseColor("#E57B35"),
        "BlueSwordswoman" to android.graphics.Color.parseColor("#83D4FF"),
        "Rukia" to android.graphics.Color.parseColor("#20232E"),
        "Rebecca" to android.graphics.Color.parseColor("#8DD7D0"),
        "Orihime" to android.graphics.Color.parseColor("#EF944F"),
        "Noelle" to android.graphics.Color.parseColor("#B8B3F5"),
        "Asuka" to android.graphics.Color.parseColor("#C96639"),
        "Alice" to android.graphics.Color.parseColor("#F4D377"),
        "Hinata" to android.graphics.Color.parseColor("#353049"),
        "Hancock" to android.graphics.Color.parseColor("#24202A"),
        "Nezuko" to android.graphics.Color.parseColor("#262126"),
        "ZeroTwo" to android.graphics.Color.parseColor("#F49AB2"),
        "Nobara" to android.graphics.Color.parseColor("#885A45"),
        "Mitsuri" to android.graphics.Color.parseColor("#EEA6C5"),
        "Yamato" to android.graphics.Color.parseColor("#F2F4FA"),
        "Esil" to android.graphics.Color.parseColor("#6851A4"),
        "Shinobu" to android.graphics.Color.parseColor("#242039"),
        "Mimosa" to android.graphics.Color.parseColor("#F5C37C"),
        "Alpha" to android.graphics.Color.parseColor("#F5D99C"),
        "Rias" to android.graphics.Color.parseColor("#B62E48"),
        "Shuna" to android.graphics.Color.parseColor("#F3A7D0"),
        "Luna" to android.graphics.Color.parseColor("#B8B1E7")
    )
    fun render(name:String,mood:String):Bitmap {
        val image=Bitmap.createBitmap(256,320,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(image)
        val p=Paint(Paint.ANTI_ALIAS_FLAG)
        fun oval(c:Int,l:Float,t:Float,r:Float,b:Float) {
            p.color=c;canvas.drawOval(RectF(l,t,r,b),p)
        }
        fun circle(c:Int,x:Float,y:Float,r:Float) {
            p.color=c;canvas.drawCircle(x,y,r,p)
        }
        fun rect(c:Int,l:Float,t:Float,r:Float,b:Float,rad:Float) {
            p.color=c;canvas.drawRoundRect(RectF(l,t,r,b),rad,rad,p)
        }
        val h=hair[name] ?: Color.rgb(125,95,185)
        val outfit=when(name) {
            "Aqua","Noelle","Alice" -> Color.rgb(65,114,205)
            "Erza","Rias","Asuka","Nobara" -> Color.rgb(150,45,70)
            "Nezuko","Mitsuri","Shuna" -> Color.rgb(198,95,145)
            else -> Color.rgb(63,51,106)
        }
        // Legs, shoes, arms, dress and sleeves.
        rect(Color.rgb(242,193,169),93f,258f,111f,300f,9f)
        rect(Color.rgb(242,193,169),145f,258f,163f,300f,9f)
        oval(Color.rgb(40,35,67),84f,292f,119f,308f)
        oval(Color.rgb(40,35,67),138f,292f,172f,308f)
        oval(outfit,66f,205f,190f,283f)
        oval(outfit,47f,210f,86f,263f)
        oval(outfit,172f,210f,210f,263f)
        circle(Color.rgb(248,204,182),62f,254f,13f)
        circle(Color.rgb(248,204,182),195f,254f,13f)
        // Distinct silhouettes and signature accessories.
        when(name) {
            "Aqua" -> { oval(h,103f,1f,154f,49f);circle(Color.rgb(44,153,223),128f,11f,13f) }
            "Tenten" -> {circle(h,48f,49f,29f);circle(h,207f,49f,29f)}
            "Temari" -> {for(x in listOf(43f,87f,170f,211f)) circle(h,x,51f,25f)}
            "Shinobu" -> {
                oval(Color.rgb(145,113,203),23f,42f,89f,99f)
                oval(Color.rgb(145,113,203),167f,42f,233f,99f)
                circle(Color.rgb(99,217,200),44f,67f,11f)
                circle(Color.rgb(99,217,200),211f,67f,11f)
            }
            "Rebecca" -> {circle(h,48f,60f,27f);circle(h,208f,60f,27f)}
            "Nami","Orihime","Rias","Hancock" -> {
                oval(h,34f,139f,72f,277f);oval(h,183f,139f,221f,277f)
            }
            "Mitsuri" -> {
                oval(Color.rgb(143,201,125),28f,153f,75f,269f)
                oval(Color.rgb(143,201,125),181f,153f,228f,269f)
            }
            "Nezuko" -> {
                oval(Color.rgb(177,91,64),43f,164f,77f,255f)
                oval(Color.rgb(177,91,64),178f,164f,212f,255f)
            }
            "ZeroTwo","Yamato" -> {
                p.color=if(name=="ZeroTwo") Color.rgb(204,70,91) else Color.rgb(173,84,104)
                val left=Path().apply {moveTo(70f,55f);lineTo(82f,5f);lineTo(94f,55f);close()}
                val right=Path().apply {moveTo(162f,55f);lineTo(174f,5f);lineTo(186f,55f);close()}
                canvas.drawPath(left,p);canvas.drawPath(right,p)
            }
        }
        // Hair behind head and face.
        oval(h,40f,20f,216f,222f)
        oval(Color.rgb(251,214,191),56f,43f,201f,210f)
        oval(h,47f,24f,209f,100f)
        oval(h,45f,52f,78f,174f)
        oval(h,180f,52f,211f,174f)
        // Eyes and blush.
        val eye=if(name in listOf("Aqua","Noelle","Alice","Alpha","Luna")) Color.rgb(65,132,206)
                else Color.rgb(119,68,111)
        if(mood=="victory" || mood=="excited") {
            p.color=Color.rgb(48,39,63);p.style=Paint.Style.STROKE;p.strokeWidth=5f
            canvas.drawArc(RectF(79f,121f,110f,146f),190f,155f,false,p)
            canvas.drawArc(RectF(146f,121f,177f,146f),190f,155f,false,p)
            p.style=Paint.Style.FILL
        } else {
            oval(Color.WHITE,78f,116f,112f,156f)
            oval(Color.WHITE,143f,116f,177f,156f)
            oval(eye,88f,121f,107f,153f)
            oval(eye,152f,121f,171f,153f)
            circle(Color.WHITE,95f,127f,5f);circle(Color.WHITE,159f,127f,5f)
        }
        oval(0x77FF799A,66f,159f,91f,170f)
        oval(0x77FF799A,166f,159f,191f,170f)
        p.color=Color.rgb(103,47,74);p.style=Paint.Style.STROKE;p.strokeWidth=4f
        val mouth=RectF(117f,165f,140f,184f)
        canvas.drawArc(mouth,if(mood=="surprised") 0f else 10f,
            if(mood=="surprised") 360f else 160f,false,p)
        p.style=Paint.Style.FILL
        // Costume details that remain recognizable in a small game portrait.
        when(name) {
            "Aqua" -> {rect(Color.WHITE,121f,225f,135f,273f,4f);circle(Color.YELLOW,128f,238f,8f)}
            "Shinobu" -> {
                oval(Color.rgb(226,223,248),61f,208f,104f,254f)
                oval(Color.rgb(226,223,248),152f,208f,195f,254f)
            }
            "Nezuko" -> {
                rect(Color.rgb(230,151,173),109f,230f,148f,274f,5f)
                rect(Color.rgb(70,137,93),112f,158f,146f,170f,5f)
            }
            "Asuka" -> {oval(Color.rgb(209,65,55),81f,227f,175f,277f)}
            "Erza","Yamato" -> {rect(Color.rgb(207,217,230),102f,231f,153f,269f,7f)}
            "ZeroTwo" -> {rect(Color.WHITE,96f,230f,159f,275f,10f)}
        }
        // Collar and small decorative bow.
        oval(Color.WHITE,104f,207f,130f,224f)
        oval(Color.WHITE,129f,207f,155f,224f)
        circle(Color.rgb(244,175,95),129f,220f,6f)
        return image
    }
}
