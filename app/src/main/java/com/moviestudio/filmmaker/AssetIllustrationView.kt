package com.moviestudio.filmmaker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import kotlin.math.min

/**
 * Lightweight 2D catalogue artwork. These are temporary illustrated previews,
 * not photographs and not 3D models. Drawn locally; no network or media upload.
 */
class AssetIllustrationView(
    context: Context,
    private val categoryId: String,
    private val variant: Int
) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private fun color(c: Int) { p.color = c; p.style = Paint.Style.FILL }
    private fun oval(c: Canvas, x: Float, y: Float, rx: Float, ry: Float, col: Int) {
        color(col); c.drawOval(x-rx, y-ry, x+rx, y+ry, p)
    }
    private fun rect(c: Canvas, l: Float, t: Float, r: Float, b: Float, col: Int, radius: Float = 0f) {
        color(col)
        if (radius > 0f) c.drawRoundRect(l,t,r,b,radius,radius,p) else c.drawRect(l,t,r,b,p)
    }
    private fun line(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, col: Int, width: Float) {
        color(col); p.strokeWidth=width; p.strokeCap=Paint.Cap.ROUND; c.drawLine(x1,y1,x2,y2,p)
    }
    private fun polygon(c: Canvas, pts: FloatArray, col: Int) {
        color(col); val path=Path(); path.moveTo(pts[0],pts[1])
        var i=2; while(i<pts.size){ path.lineTo(pts[i],pts[i+1]); i+=2 }
        path.close(); c.drawPath(path,p)
    }
    private fun scaled(canvas: Canvas, block: (Canvas,Float,Float)->Unit) {
        val side=min(width.toFloat(),height.toFloat()).coerceAtLeast(1f)
        val save=canvas.save()
        canvas.translate((width-side)/2f,(height-side)/2f)
        canvas.scale(side/320f,side/320f)
        block(canvas,320f,320f)
        canvas.restoreToCount(save)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        scaled(canvas) { c, _, _ ->
            val palettes = intArrayOf(0xFF5B8DB8.toInt(),0xFF9275B8.toInt(),0xFF4D9A83.toInt(),0xFFB47A52.toInt(),0xFF5678A8.toInt())
            val accent=palettes[variant%palettes.size]
            rect(c,0f,0f,320f,320f,0xFF17243A.toInt(),24f)
            when(categoryId) {
                MovieAssetLibrary.CHARACTERS -> drawPerson(c,accent)
                MovieAssetLibrary.CREATURES -> drawCreature(c,accent)
                MovieAssetLibrary.ENVIRONMENTS -> drawLandscape(c,accent)
                else -> drawBuilding(c,accent)
            }
        }
    }

    private fun drawPerson(c: Canvas, accent: Int) {
        val skin=if(variant%3==0) 0xFFB97955.toInt() else if(variant%3==1) 0xFFE0B18B.toInt() else 0xFF8D5D43.toInt()
        val cloth= intArrayOf(accent,0xFF9B4D55.toInt(),0xFF477A65.toInt(),0xFFB28A43.toInt())[variant%4]
        // shadow, legs, body, arms, head, hair and simple clothing detail
        oval(c,160f,278f,76f,13f,0x66000000)
        line(c,143f,222f,132f,274f,0xFF29384D.toInt(),19f)
        line(c,178f,222f,190f,274f,0xFF29384D.toInt(),19f)
        polygon(c,floatArrayOf(118f,128f,202f,128f,214f,232f,106f,232f),cloth)
        line(c,119f,145f,91f,206f,skin,17f)
        line(c,201f,145f,229f,206f,skin,17f)
        oval(c,160f,91f,37f,42f,skin)
        oval(c,160f,58f,38f,17f,0xFF332B2B.toInt())
        rect(c,139f,82f,181f,91f,skin)
        line(c,147f,99f,153f,99f,0xFF263243.toInt(),4f)
        line(c,168f,99f,174f,99f,0xFF263243.toInt(),4f)
        line(c,151f,116f,169f,116f,0xFF6D3434.toInt(),3f)
        if(variant%5==6) polygon(c,floatArrayOf(130f,55f,190f,55f,182f,34f,139f,34f),0xFFFFD36C.toInt())
        rect(c,143f,160f,177f,170f,0x66FFFFFF,3f)
        oval(c,160f,160f,5f,5f,0xFFFFD36C.toInt())
    }

    private fun drawCreature(c: Canvas, accent: Int) {
        val fur=intArrayOf(0xFFC58A45.toInt(),0xFF777C86.toInt(),0xFFB77A54.toInt(),0xFFDAA85A.toInt())[variant%4]
        oval(c,160f,275f,92f,12f,0x66000000)
        // body, head, ears, legs and tail
        oval(c,153f,174f,83f,48f,fur)
        oval(c,222f,139f,39f,35f,fur)
        oval(c,199f,107f,15f,23f,fur)
        oval(c,244f,107f,15f,23f,fur)
        line(c,103f,169f,67f,137f,fur,13f)
        line(c,112f,197f,105f,257f,fur,15f)
        line(c,154f,204f,151f,260f,fur,15f)
        line(c,194f,202f,199f,257f,fur,15f)
        line(c,226f,194f,238f,250f,fur,15f)
        oval(c,238f,145f,4f,5f,0xFF17202C.toInt())
        oval(c,253f,159f,13f,8f,0xFF3C2930.toInt())
        if(variant%4==1) {
            polygon(c,floatArrayOf(118f,145f,139f,82f,157f,147f),accent)
            polygon(c,floatArrayOf(151f,135f,178f,77f,183f,145f),accent)
        }
        if(variant%4==2) line(c,80f,139f,54f,107f,accent,7f)
        oval(c,160f,180f,30f,20f,0x22FFFFFF)
    }

    private fun drawLandscape(c: Canvas, accent: Int) {
        rect(c,0f,0f,320f,320f,0xFF8DB8D0.toInt(),22f)
        oval(c,250f,58f,25f,25f,0xFFFFD36C.toInt())
        polygon(c,floatArrayOf(0f,210f,82f,93f,164f,210f),0xFF536F82.toInt())
        polygon(c,floatArrayOf(100f,212f,211f,75f,320f,214f),0xFF3E596E.toInt())
        polygon(c,floatArrayOf(55f,132f,82f,93f,111f,136f),0xFFE6EDF0.toInt())
        polygon(c,floatArrayOf(175f,120f,211f,75f,249f,122f),0xFFE6EDF0.toInt())
        rect(c,0f,210f,320f,320f,0xFF56845A.toInt())
        if(variant%4==0) {
            rect(c,0f,230f,320f,320f,0xFF367EA0.toInt())
            line(c,0f,244f,320f,244f,0xFF9DD4E5.toInt(),4f)
        } else {
            for(i in 0..4) {
                val x=28f+i*68f
                rect(c,x,184f,x+12f,255f,0xFF604A35.toInt())
                oval(c,x+6f,170f,30f,39f,if(i%2==0) 0xFF286448.toInt() else 0xFF39764D.toInt())
            }
        }
        line(c,20f,284f,300f,284f,accent,4f)
    }

    private fun drawBuilding(c: Canvas, accent: Int) {
        rect(c,0f,0f,320f,320f,0xFF91B7D0.toInt(),22f)
        rect(c,0f,248f,320f,320f,0xFF657C54.toInt())
        oval(c,255f,52f,23f,23f,0xFFFFD36C.toInt())
        val wall=intArrayOf(0xFFD7B98C.toInt(),0xFFB8C3CC.toInt(),0xFFD2D0C5.toInt(),0xFFB98262.toInt())[variant%4]
        rect(c,70f,111f,250f,260f,wall,5f)
        polygon(c,floatArrayOf(48f,116f,160f,42f,272f,116f),accent)
        rect(c,137f,188f,183f,260f,0xFF654A3D.toInt(),4f)
        rect(c,88f,145f,125f,183f,0xFF76B5D0.toInt(),4f)
        rect(c,195f,145f,232f,183f,0xFF76B5D0.toInt(),4f)
        line(c,106f,145f,106f,183f,0xFFEAF4F7.toInt(),3f)
        line(c,213f,145f,213f,183f,0xFFEAF4F7.toInt(),3f)
        if(variant%3==1) {
            rect(c,151f,24f,169f,76f,wall)
            polygon(c,floatArrayOf(143f,27f,160f,4f,177f,27f),0xFFB9C4D0.toInt())
        }
        if(variant%3==2) {
            rect(c,38f,215f,282f,228f,0xFF8B7659.toInt())
            line(c,40f,225f,280f,225f,0xFFDDC59A.toInt(),3f)
        }
    }
}
