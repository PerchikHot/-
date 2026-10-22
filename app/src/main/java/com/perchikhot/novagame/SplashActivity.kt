package com.perchikhot.novagame

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.*
import android.view.View

class SplashActivity:Activity() {
    private val handler=Handler(Looper.getMainLooper())
    private val next=Runnable {
        if(!isFinishing) {
            startActivity(Intent(this,MenuActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out)
            finish()
        }
    }
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(SplashView())
        handler.postDelayed(next,2400)
    }
    override fun onDestroy() {handler.removeCallbacks(next);super.onDestroy()}
    private inner class SplashView:View(this@SplashActivity) {
        private val p=Paint(Paint.ANTI_ALIAS_FLAG)
        private val started=android.os.SystemClock.uptimeMillis()
        private val photoId=resources.getIdentifier("splash_photo","drawable",packageName)
        private val photo=if(photoId!=0) BitmapFactory.decodeResource(resources,photoId) else null
        override fun onDraw(c:Canvas) {
            val w=width.toFloat()
            val h=height.toFloat()
            val t=(android.os.SystemClock.uptimeMillis()-started)/1000f
            c.drawColor(0xFF0B0C25.toInt())
            if(photo!=null) {
                val caption=(h*.17f).coerceAtLeast(110f*resources.displayMetrics.density)
                val available=h-caption
                val scale=minOf(w/photo.width,available/photo.height)
                val pw=photo.width*scale
                val ph=photo.height*scale
                p.color=Color.WHITE
                c.drawBitmap(photo,null,RectF((w-pw)/2,(available-ph)/2,(w+pw)/2,(available+ph)/2),p)
                p.color=0xFF16122E.toInt()
                c.drawRect(0f,available,w,h,p)
                val phase=(t%2.4f)/2.4f
                p.color=if(phase<.5f) 0xFFD6B7FF.toInt() else 0xFFFF8BCD.toInt()
                p.textAlign=Paint.Align.CENTER
                p.typeface=Typeface.create("sans-serif-black",Typeface.BOLD)
                p.textSize=w*.083f
                c.drawText(if(phase<.5f) "NOVA GAME" else "FUTA GAME",w*.5f,available+caption*.50f,p)
                p.color=0xFF433759.toInt()
                c.drawRoundRect(w*.17f,available+caption*.73f,w*.83f,available+caption*.77f,8f,8f,p)
                p.color=0xFFB975FA.toInt()
                c.drawRoundRect(w*.17f,available+caption*.73f,w*(.17f+.66f*(t/2.4f).coerceIn(0f,1f)),available+caption*.77f,8f,8f,p)
                postInvalidateDelayed(33)
                return
            }
            p.shader=LinearGradient(0f,0f,w,h,0xFF30235B.toInt(),0xFF0A1029.toInt(),Shader.TileMode.CLAMP)
            c.drawRoundRect(w*.09f,h*.11f,w*.91f,h*.89f,44f,44f,p)
            p.shader=null
            p.color=0xFF15112C.toInt()
            val shadow=Path().apply {
                moveTo(w*.25f,h*.65f)
                cubicTo(w*.22f,h*.46f,w*.33f,h*.34f,w*.40f,h*.32f)
                lineTo(w*.33f,h*.20f)
                lineTo(w*.48f,h*.27f)
                lineTo(w*.69f,h*.20f)
                lineTo(w*.65f,h*.33f)
                cubicTo(w*.80f,h*.46f,w*.80f,h*.57f,w*.75f,h*.65f)
                close()
            }
            c.drawPath(shadow,p)
            p.color=0xFFFF416C.toInt()
            c.drawOval(w*.37f,h*.39f,w*.46f,h*.405f,p)
            c.drawOval(w*.56f,h*.39f,w*.65f,h*.405f,p)
            for(i in 0..2) {
                val x=w*(.40f+i*.10f)
                val y=h*(.47f+((t*.16f+i*.19f)%1f)*.18f)
                p.color=if(i==1) 0xFFFF65B4.toInt() else 0xFFA05CFA.toInt()
                p.strokeWidth=w*.014f
                p.strokeCap=Paint.Cap.ROUND
                p.style=Paint.Style.STROKE
                val path=Path().apply {
                    moveTo(x,h*.43f)
                    cubicTo(x-w*.03f,h*.48f,x+w*.03f,y-h*.06f,x,y)
                }
                c.drawPath(path,p)
                p.style=Paint.Style.FILL
                c.drawCircle(x,y,w*.013f,p)
            }
            p.color=Color.WHITE
            p.textAlign=Paint.Align.CENTER
            p.typeface=Typeface.create("sans-serif-black",Typeface.BOLD)
            p.textSize=w*.083f
            c.drawText("NOVA GAME",w*.5f,h*.75f,p)
            p.textSize=w*.035f
            p.typeface=Typeface.DEFAULT
            p.color=0xFFD6CAFF.toInt()
            c.drawText("ТВОЯ КОЛЛЕКЦИЯ • ТВОЯ ИГРА",w*.5f,h*.79f,p)
            p.color=0xFF343050.toInt()
            c.drawRoundRect(w*.18f,h*.82f,w*.82f,h*.828f,7f,7f,p)
            p.color=0xFFBD74FF.toInt()
            c.drawRoundRect(w*.18f,h*.82f,w*(.18f+.64f*(t/2.4f).coerceIn(0f,1f)),h*.828f,7f,7f,p)
            postInvalidateDelayed(33)
        }
    }
}
