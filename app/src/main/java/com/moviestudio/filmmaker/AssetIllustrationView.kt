package com.moviestudio.filmmaker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import kotlin.math.min

/**
 * Locally rendered, high-detail stylised game-art thumbnails.
 * These are bespoke 2D illustrations, not photographs or 3D meshes.
 */
class AssetIllustrationView(
    context: Context,
    private val categoryId: String,
    private val variant: Int
) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val palettes = intArrayOf(
        0xFF5F91C9.toInt(), 0xFFB56D54.toInt(), 0xFF6A9B76.toInt(),
        0xFF9B79B9.toInt(), 0xFFC49A54.toInt(), 0xFF4C9C9C.toInt()
    )

    private fun fill(c: Canvas, col: Int) { p.shader=null; p.color=col; p.style=Paint.Style.FILL }
    private fun rect(c: Canvas,l:Float,t:Float,r:Float,b:Float,col:Int,rad:Float=0f) {
        fill(c,col); if(rad>0f)c.drawRoundRect(l,t,r,b,rad,rad,p) else c.drawRect(l,t,r,b,p)
    }
    private fun oval(c:Canvas,x:Float,y:Float,rx:Float,ry:Float,col:Int) {
        fill(c,col); c.drawOval(x-rx,y-ry,x+rx,y+ry,p)
    }
    private fun stroke(c:Canvas,x1:Float,y1:Float,x2:Float,y2:Float,col:Int,w:Float) {
        fill(c,col); p.style=Paint.Style.STROKE; p.strokeWidth=w; p.strokeCap=Paint.Cap.ROUND
        c.drawLine(x1,y1,x2,y2,p); p.style=Paint.Style.FILL
    }
    private fun poly(c:Canvas, points:FloatArray,col:Int) {
        fill(c,col); val q=Path(); q.moveTo(points[0],points[1]); var i=2
        while(i<points.size){q.lineTo(points[i],points[i+1]);i+=2}; q.close(); c.drawPath(q,p)
    }
    private fun gradient(c:Canvas,top:Int,bottom:Int) {
        p.shader=LinearGradient(0f,0f,0f,320f,top,bottom,Shader.TileMode.CLAMP)
        p.style=Paint.Style.FILL; c.drawRect(0f,0f,320f,320f,p); p.shader=null
    }
    private fun detail(c:Canvas,x:Float,y:Float,w:Float,col:Int) {
        stroke(c,x,y,x+w,y,col,2.2f)
    }
    private fun scaled(canvas:Canvas,draw:(Canvas)->Unit) {
        val w=width.toFloat().coerceAtLeast(1f)
        val h=height.toFloat().coerceAtLeast(1f)
        val save=canvas.save()
        if (categoryId == MovieAssetLibrary.ENVIRONMENTS || categoryId == MovieAssetLibrary.BUILDINGS) {
            // Landscape categories fill a wide cinematic frame instead of being squeezed into a square.
            canvas.scale(w/320f,h/320f)
        } else {
            val side=min(w,h)
            canvas.translate((w-side)/2f,(h-side)/2f)
            canvas.scale(side/320f,side/320f)
        }
        draw(canvas)
        canvas.restoreToCount(save)
    }

    override fun onDraw(canvas:Canvas) {
        super.onDraw(canvas)
        scaled(canvas) { c ->
            // Deep illustrated-card frame, soft vignette and ground shadow.
            rect(c,0f,0f,320f,320f,0xFF101B2B.toInt(),24f)
            when(categoryId) {
                MovieAssetLibrary.CHARACTERS -> drawCharacter(c)
                MovieAssetLibrary.CREATURES -> drawCreature(c)
                MovieAssetLibrary.ENVIRONMENTS -> drawEnvironment(c)
                else -> drawArchitecture(c)
            }
            // Thin inset frame and small glints give every tile a finished game-card look.
            fill(c,0x55FFFFFF); p.style=Paint.Style.STROKE; p.strokeWidth=2f
            c.drawRoundRect(5f,5f,315f,315f,20f,20f,p); p.style=Paint.Style.FILL
            oval(c,286f,28f,3f,3f,0x99FFFFFF.toInt())
        }
    }

    private fun drawCharacter(c:Canvas) {
        val v=variant%20
        val skin=intArrayOf(0xFFB87858.toInt(),0xFFE0B18A.toInt(),0xFF8D5B43.toInt(),0xFFC98D6B.toInt())[v%4]
        val cloth=palettes[v%palettes.size]
        val dark=0xFF263449.toInt()
        gradient(c,0xFF263B57.toInt(),0xFF101A2A.toInt())
        // distant atmospheric arches and floor
        oval(c,160f,148f,116f,116f,0x222CFFFF.toInt())
        rect(c,0f,265f,320f,320f,0xFF182536.toInt())
        oval(c,160f,278f,77f,12f,0x88000000.toInt())
        // legs, boots, and silhouette vary with the profession/role
        val leg=if(v==6||v==7||v==8) 0xFF44334B.toInt() else dark
        stroke(c,143f,214f,133f,265f,leg,20f); stroke(c,178f,214f,188f,265f,leg,20f)
        rect(c,116f,258f,146f,277f,0xFF111923.toInt(),7f)
        rect(c,176f,258f,205f,277f,0xFF111923.toInt(),7f)
        // coat / tunic / armour with layered highlights
        poly(c,floatArrayOf(119f,128f,201f,128f,218f,232f,102f,232f),cloth)
        poly(c,floatArrayOf(119f,128f,160f,151f,201f,128f,190f,225f,130f,225f),0x33202B3C)
        stroke(c,160f,150f,160f,224f,0x99FFFFFF.toInt(),3f)
        stroke(c,119f,146f,91f,205f,skin,18f); stroke(c,201f,146f,229f,205f,skin,18f)
        oval(c,90f,208f,10f,12f,skin); oval(c,230f,208f,10f,12f,skin)
        // waist belt and emblem
        rect(c,119f,197f,201f,207f,0xFF493B32.toInt(),3f)
        oval(c,160f,202f,7f,7f,0xFFFFD477.toInt())
        // head, ears, neck
        rect(c,148f,116f,172f,140f,skin,7f)
        oval(c,160f,88f,35f,42f,skin); oval(c,125f,91f,6f,10f,skin); oval(c,195f,91f,6f,10f,skin)
        // hair and headgear are deliberately different across the catalogue
        when(v) {
            6,7,8 -> { // royal crown
                poly(c,floatArrayOf(128f,61f,135f,31f,151f,48f,160f,22f,171f,48f,187f,31f,192f,62f),0xFFFFD45F.toInt())
                rect(c,130f,58f,190f,68f,0xFFB57B35.toInt(),3f)
                oval(c,160f,30f,5f,5f,0xFFFFF0B0.toInt())
            }
            9,10,11 -> { // helmet / cap
                oval(c,160f,66f,40f,22f,0xFF394C62.toInt())
                rect(c,124f,66f,196f,75f,0xFF263449.toInt(),5f)
                poly(c,floatArrayOf(156f,43f,164f,43f,160f,17f),0xFFFFD36C.toInt())
            }
            12,13 -> { oval(c,160f,61f,39f,17f,0xFFF0F1E9.toInt()); rect(c,124f,62f,196f,71f,0xFFD5DCD9.toInt(),4f) }
            15,16,17 -> { oval(c,160f,63f,38f,20f,0xFF463B32.toInt()); rect(c,125f,58f,195f,70f,0xFF463B32.toInt(),5f) }
            else -> { oval(c,160f,58f,38f,19f,0xFF302A2A.toInt()); poly(c,floatArrayOf(126f,68f,132f,45f,155f,52f,180f,43f,194f,68f),0xFF302A2A.toInt()) }
        }
        // eyes, brows, nose and mouth
        oval(c,147f,91f,3.5f,4f,0xFF17202C.toInt()); oval(c,173f,91f,3.5f,4f,0xFF17202C.toInt())
        stroke(c,141f,82f,153f,80f,0xFF49342F.toInt(),2.5f); stroke(c,167f,80f,179f,82f,0xFF49342F.toInt(),2.5f)
        stroke(c,160f,94f,157f,105f,0x886A4639.toInt(),2f); stroke(c,153f,113f,167f,113f,0xFF713E3A.toInt(),2f)
        // role-specific equipment and garment details
        when(v) {
            9 -> { stroke(c,208f,146f,230f,245f,0xFFB7C6D4.toInt(),7f); rect(c,220f,235f,239f,252f,0xFF36475C.toInt(),4f) }
            10,11 -> { rect(c,111f,133f,209f,150f,0xFF4A5E42.toInt(),4f); stroke(c,207f,153f,236f,220f,0xFF8C9B6C.toInt(),5f) }
            12,13 -> { rect(c,143f,158f,177f,183f,0xFFF1F0E7.toInt(),5f); poly(c,floatArrayOf(160f,160f,149f,177f,160f,173f,171f,177f),0xFFCB4F4F.toInt()) }
            14 -> { rect(c,143f,157f,177f,190f,0xFFEEE9D9.toInt(),3f); stroke(c,145f,167f,175f,167f,0xFFB64D45.toInt(),2f) }
            18 -> { poly(c,floatArrayOf(120f,135f,160f,122f,200f,135f,192f,220f,128f,220f),0xFFB64042.toInt()); oval(c,160f,168f,12f,12f,0xFFFFD46A.toInt()) }
            19 -> { rect(c,121f,150f,199f,161f,0xFFB58C4C.toInt(),4f); stroke(c,128f,162f,191f,162f,0xFFFFD77B.toInt(),2f) }
            else -> { detail(c,133f,177f,54f,0x88FFFFFF.toInt()); detail(c,133f,185f,54f,0x66FFFFFF.toInt()) }
        }
        // Distinct full-body silhouettes and profession-specific equipment.
        // Every role receives its own outline, footwear and readable prop.
        when(v) {
            0 -> { // everyday man: broad shoulders, long trousers, open jacket
                poly(c,floatArrayOf(118f,130f,202f,130f,211f,226f,109f,226f),0xFF31506C.toInt())
                stroke(c,137f,222f,128f,265f,0xFF202B3B.toInt(),17f)
                stroke(c,183f,222f,192f,265f,0xFF202B3B.toInt(),17f)
                rect(c,111f,263f,143f,279f,0xFF171D26.toInt(),6f); rect(c,177f,263f,209f,279f,0xFF171D26.toInt(),6f)
            }
            1 -> { // woman: long coat and shaped silhouette
                poly(c,floatArrayOf(137f,130f,183f,130f,214f,239f,106f,239f),0xFF8B4F72.toInt())
                stroke(c,143f,230f,139f,267f,0xFF29283A.toInt(),15f); stroke(c,177f,230f,181f,267f,0xFF29283A.toInt(),15f)
                rect(c,123f,263f,151f,278f,0xFF33283B.toInt(),7f); rect(c,169f,263f,197f,278f,0xFF33283B.toInt(),7f)
                stroke(c,135f,150f,112f,204f,skin,15f); stroke(c,185f,150f,208f,204f,skin,15f)
            }
            2,3 -> { // children: shorter torso and shorter legs
                oval(c,160f,180f,43f,51f,cloth)
                stroke(c,144f,211f,141f,254f,leg,15f); stroke(c,176f,211f,179f,254f,leg,15f)
                rect(c,126f,248f,151f,263f,0xFF171D26.toInt(),6f); rect(c,169f,248f,194f,263f,0xFF171D26.toInt(),6f)
                stroke(c,121f,149f,105f,190f,skin,13f); stroke(c,199f,149f,215f,190f,skin,13f)
            }
            4,5 -> { // elders: stooped posture, shawl and walking stick
                stroke(c,145f,147f,126f,210f,skin,15f); stroke(c,190f,147f,207f,208f,skin,15f)
                stroke(c,206f,170f,220f,270f,0xFF9A7952.toInt(),5f)
                poly(c,floatArrayOf(115f,133f,204f,133f,221f,231f,104f,231f),0xFF786C63.toInt())
                stroke(c,145f,222f,139f,265f,0xFF3D3A39.toInt(),16f); stroke(c,180f,222f,185f,265f,0xFF3D3A39.toInt(),16f)
            }
            6,7,8 -> { // royal figures: capes, decorated hems and boots
                poly(c,floatArrayOf(126f,131f,194f,131f,238f,239f,82f,239f),if(v==6)0xFF663F54.toInt() else if(v==7)0xFF3E557C.toInt() else 0xFF865D8D.toInt())
                for(x in listOf(116f,132f,148f,164f,180f,196f)) oval(c,x,226f,3f,4f,0xFFFFD66F.toInt())
                rect(c,119f,260f,151f,280f,0xFF29253A.toInt(),5f); rect(c,169f,260f,201f,280f,0xFF29253A.toInt(),5f)
            }
            9,10,11 -> { // public safety and military: body armor, pouches, sturdy boots
                rect(c,120f,139f,200f,215f,0xFF455C4B.toInt(),9f)
                for(x in listOf(128f,151f,174f)) rect(c,x,175f,x+15f,194f,0xFF263A32.toInt(),3f)
                stroke(c,140f,216f,132f,266f,0xFF26313A.toInt(),19f); stroke(c,180f,216f,188f,266f,0xFF26313A.toInt(),19f)
                rect(c,112f,260f,147f,281f,0xFF171D26.toInt(),5f); rect(c,175f,260f,210f,281f,0xFF171D26.toInt(),5f)
            }
            12,13 -> { // medical staff: coat, collar and medical emblem
                poly(c,floatArrayOf(125f,130f,195f,130f,211f,231f,109f,231f),0xFFE7E8E1.toInt())
                stroke(c,160f,139f,160f,220f,0xFF9FA9A7.toInt(),2f)
                rect(c,147f,158f,173f,184f,0xFFB8474D.toInt(),4f)
                rect(c,156f,163f,164f,179f,Color.WHITE,1f); rect(c,151f,167f,169f,175f,Color.WHITE,1f)
            }
            14 -> { // teacher: jacket, book and spectacles
                rect(c,122f,137f,198f,224f,0xFF5D647B.toInt(),12f)
                rect(c,202f,177f,230f,215f,0xFFB77E43.toInt(),3f)
                stroke(c,143f,264f,139f,278f,0xFF171D26.toInt(),8f); stroke(c,180f,264f,184f,278f,0xFF171D26.toInt(),8f)
            }
            15,16,17 -> { // farmer, builder, engineer: workwear and tools
                rect(c,119f,139f,201f,222f,0xFF786348.toInt(),8f)
                rect(c,130f,145f,148f,190f,0xFFB8A27A.toInt(),3f); rect(c,172f,145f,190f,190f,0xFFB8A27A.toInt(),3f)
                stroke(c,207f,155f,238f,230f,0xFF9D9A8D.toInt(),5f)
                rect(c,115f,260f,150f,281f,0xFF352F2B.toInt(),5f); rect(c,173f,260f,208f,281f,0xFF352F2B.toInt(),5f)
            }
            18 -> { // hero: armored chest, shoulder guards and long boots
                poly(c,floatArrayOf(118f,130f,202f,130f,220f,226f,100f,226f),0xFF9D343F.toInt())
                oval(c,119f,145f,17f,17f,0xFFC5A35C.toInt()); oval(c,201f,145f,17f,17f,0xFFC5A35C.toInt())
                poly(c,floatArrayOf(160f,153f,174f,180f,160f,205f,146f,180f),0xFFFFD36D.toInt())
                rect(c,114f,259f,149f,282f,0xFF252B39.toInt(),5f); rect(c,171f,259f,206f,282f,0xFF252B39.toInt(),5f)
            }
            19 -> { // merchant: layered vest and shoulder satchel
                rect(c,123f,138f,197f,222f,0xFF9A744B.toInt(),8f)
                stroke(c,196f,150f,224f,196f,0xFF6B4935.toInt(),5f)
                rect(c,211f,188f,242f,222f,0xFF8D5C3E.toInt(),5f)
                stroke(c,140f,221f,136f,266f,0xFF34333B.toInt(),16f); stroke(c,180f,221f,184f,266f,0xFF34333B.toInt(),16f)
            }
        }
        // Visible hands, fingers, separate boots and garment seams complete the full-body read.
        oval(c,89f,210f,9f,11f,skin); oval(c,231f,210f,9f,11f,skin)
        stroke(c,85f,211f,81f,218f,0xFF9B6B52.toInt(),1.8f)
        stroke(c,235f,211f,239f,218f,0xFF9B6B52.toInt(),1.8f)
        stroke(c,121f,232f,199f,232f,0x66FFFFFF.toInt(),1.5f)
        // small cinematic rim lights
        stroke(c,121f,139f,112f,185f,0x66FFFFFF.toInt(),2f)
        stroke(c,199f,139f,208f,185f,0x66FFFFFF.toInt(),2f)
    }

    private fun drawCreature(c:Canvas) {
        val v=variant%20
        val fur=intArrayOf(0xFFC18A4E.toInt(),0xFF8C8C91.toInt(),0xFFB77A55.toInt(),0xFFD0A45E.toInt(),0xFF76513C.toInt())[v%5]
        gradient(c,0xFF34465D.toInt(),0xFF111B2A.toInt())
        oval(c,160f,276f,103f,13f,0x99000000.toInt())
        // species-specific silhouettes, not one repeated generic animal
        when(v) {
            15,16,17 -> drawBird(c,v,fur)
            18 -> { // snake
                val path=Path(); path.moveTo(38f,235f); path.cubicTo(85f,170f,112f,270f,160f,205f); path.cubicTo(204f,145f,225f,220f,265f,152f)
                fill(c,fur); p.style=Paint.Style.STROKE; p.strokeWidth=25f; p.strokeCap=Paint.Cap.ROUND; c.drawPath(path,p); p.style=Paint.Style.FILL
                oval(c,269f,146f,24f,18f,fur); oval(c,278f,141f,3f,4f,Color.BLACK)
                stroke(c,283f,151f,299f,153f,0xFF38242B.toInt(),3f)
            }
            19 -> drawDragon(c)
            11 -> { // elephant
                oval(c,145f,175f,83f,64f,fur); oval(c,225f,149f,43f,44f,fur)
                oval(c,204f,151f,25f,35f,0xFFB6A49A.toInt())
                stroke(c,248f,168f,252f,220f,fur,19f); stroke(c,252f,220f,236f,236f,fur,15f)
                poly(c,floatArrayOf(239f,172f,252f,183f,242f,190f),0xFFF1E2CF.toInt())
                oval(c,233f,139f,4f,5f,0xFF151B24.toInt())
                for(x in listOf(94f,139f,184f,213f)) stroke(c,x,211f,x-2f,261f,fur,19f)
            }
            6,7 -> { // horse / camel
                oval(c,145f,177f,78f,42f,fur)
                stroke(c,205f,161f,229f,112f,fur,24f); oval(c,239f,104f,28f,20f,fur)
                if(v==7) { oval(c,145f,119f,18f,23f,fur); oval(c,169f,119f,18f,23f,fur) }
                else { poly(c,floatArrayOf(217f,105f,219f,75f,232f,103f),fur); poly(c,floatArrayOf(238f,98f,254f,72f,250f,106f),fur) }
                for(x in listOf(105f,145f,185f,205f)) stroke(c,x,198f,x+if(x<150f)-4f else 4f,263f,fur,12f)
                oval(c,252f,106f,3f,3f,Color.BLACK)
            }
            else -> drawQuadruped(c,v,fur)
        }
        // Species-specific markings and silhouettes keep neighbouring animals visually distinct.
        when(v) {
            0 -> { for(i in 0..6) stroke(c,191f+i*7f,112f,184f+i*7f,178f,0xFF5B3927.toInt(),3f); oval(c,210f,120f,12f,9f,0xFF8D552E.toInt()) }
            1 -> { for(i in 0..5) stroke(c,104f+i*19f,143f,115f+i*19f,194f,0xFF2E2525.toInt(),4f) }
            2 -> { oval(c,151f,137f,24f,18f,0xFFB9B8B2.toInt()); stroke(c,87f,157f,48f,132f,fur,8f) }
            3 -> { oval(c,149f,176f,18f,13f,0xFFCF9B67.toInt()); stroke(c,84f,174f,56f,151f,fur,6f) }
            4,5 -> { oval(c,204f,143f,9f,8f,0xFFE9D7C6.toInt()); stroke(c,205f,184f,235f,195f,0xFFB7A18A.toInt(),2f) }
            6 -> { oval(c,145f,141f,25f,9f,0xFF6D4935.toInt()); stroke(c,114f,135f,96f,117f,fur,4f) }
            7 -> { oval(c,145f,145f,22f,15f,0xFFB68A52.toInt()); stroke(c,104f,161f,74f,145f,fur,8f) }
            8,9,10 -> { poly(c,floatArrayOf(135f,139f,145f,104f,155f,139f),0xFFE9D8A9.toInt()); poly(c,floatArrayOf(165f,139f,177f,104f,184f,141f),0xFFE9D8A9.toInt()) }
            11 -> { oval(c,112f,145f,30f,35f,0xFFB6A49A.toInt()); stroke(c,244f,172f,244f,222f,fur,13f) }
            12 -> { poly(c,floatArrayOf(205f,115f,209f,72f,222f,118f),0xFF9A805F.toInt()); poly(c,floatArrayOf(224f,115f,251f,75f,242f,124f),0xFF9A805F.toInt()) }
            13 -> { oval(c,150f,180f,35f,29f,0xFFE7D5C2.toInt()); oval(c,226f,143f,8f,8f,0xFFE7D5C2.toInt()) }
            14 -> { stroke(c,100f,146f,84f,111f,fur,6f); stroke(c,103f,153f,75f,127f,fur,5f) }
        }
        // Hooves, paws, claws and feather highlights make the feet and surfaces readable.
        if(v !in 15..19) {
            for(x in listOf(100f,142f,183f,211f)) {
                rect(c,x-7f,258f,x+8f,270f,if(v==6||v==7||v==8||v==9||v==10)0xFF55473B.toInt() else 0xFF2B2A2B.toInt(),3f)
            }
        }
        // polished highlights and a soft contact shadow
        oval(c,143f,155f,35f,12f,0x22FFFFFF)
        stroke(c,90f,278f,230f,278f,0x448EB4D0.toInt(),2f)
    }

    private fun drawQuadruped(c:Canvas,v:Int,fur:Int) {
        val bodyX=if(v==13)145f else 151f
        oval(c,bodyX,177f,if(v==13)55f else 78f,if(v==13)48f else 44f,fur)
        val headX=when(v){0,1->225f;2,3,4,5->222f;8,9,10->218f;12->215f;else->222f}
        oval(c,headX,142f,if(v==13)30f else 34f,34f,fur)
        // ears, horns, mane and muzzle distinguish the catalogue species
        when(v) {
            0,1 -> { // lion / tiger mane
                oval(c,218f,143f,47f,47f,if(v==0)0xFF8D552E.toInt() else 0xFF6B4B3B.toInt())
                oval(c,225f,145f,29f,30f,fur)
                if(v==1) for(y in 116..172 step 12) stroke(c,203f,y.toFloat(),243f,(y+7).toFloat(),0xFF342D2D.toInt(),3f)
            }
            2,3 -> { poly(c,floatArrayOf(202f,119f,205f,88f,222f,122f),fur); poly(c,floatArrayOf(229f,119f,252f,91f,245f,130f),fur) }
            8,9,10 -> { poly(c,floatArrayOf(206f,120f,196f,79f,218f,119f),0xFFE9D7A2.toInt()); poly(c,floatArrayOf(226f,119f,241f,79f,239f,124f),0xFFE9D7A2.toInt()) }
            12 -> { poly(c,floatArrayOf(205f,121f,204f,76f,225f,119f),fur); poly(c,floatArrayOf(224f,120f,246f,76f,242f,124f),fur) }
            13 -> { oval(c,190f,111f,18f,22f,fur); oval(c,231f,111f,18f,22f,fur) }
        }
        val legs=if(v==4||v==5) 13f else 10f
        for(x in listOf(103f,145f,184f,211f)) stroke(c,x,203f,x+if(x<150f)-3f else 3f,264f,fur,legs)
        when(v) {
            0 -> { oval(c,250f,158f,23f,17f,0xFFB77B43.toInt()); stroke(c,86f,165f,48f,132f,0xFF8D552E.toInt(),9f); oval(c,48f,132f,10f,10f,fur) }
            4,5 -> { oval(c,240f,154f,22f,18f,0xFFDFB99A.toInt()); oval(c,211f,139f,3f,4f,Color.BLACK); stroke(c,90f,169f,61f,147f,fur,7f) }
            8,9,10 -> { oval(c,235f,155f,24f,17f,0xFFDDC7A5.toInt()); oval(c,225f,135f,3f,4f,Color.BLACK) }
            14 -> { oval(c,231f,151f,27f,24f,fur); oval(c,235f,145f,4f,4f,Color.BLACK) }
            else -> { oval(c,246f,153f,22f,15f,fur); oval(c,235f,137f,3f,4f,Color.BLACK) }
        }
        if(v==2||v==3) stroke(c,78f,171f,51f,150f,fur,7f)
    }

    private fun drawBird(c:Canvas,v:Int,fur:Int) {
        if(v==15) { // eagle with broad wings
            poly(c,floatArrayOf(40f,154f,112f,111f,160f,143f,208f,111f,280f,154f,208f,169f,160f,157f,112f,169f),fur)
            oval(c,160f,168f,37f,48f,fur); oval(c,189f,133f,27f,24f,0xFFE4D4B1.toInt())
            poly(c,floatArrayOf(209f,132f,245f,143f,211f,151f),0xFFFFC64F.toInt())
            for(i in 0..4) stroke(c,(75+i*24).toFloat(),151f,(108+i*15).toFloat(),165f,0x66FFFFFF.toInt(),2f)
        } else if(v==16) { // parrot
            oval(c,156f,174f,53f,69f,0xFF2B9C73.toInt()); oval(c,177f,117f,37f,38f,0xFF36B982.toInt())
            poly(c,floatArrayOf(194f,122f,236f,138f,196f,148f),0xFFFFC84F.toInt())
            poly(c,floatArrayOf(132f,219f,110f,287f,164f,246f),0xFF2878B8.toInt())
            oval(c,188f,111f,4f,4f,Color.BLACK); stroke(c,126f,158f,99f,214f,0xFFFFD65A.toInt(),8f)
        } else { // chicken
            oval(c,150f,190f,62f,48f,0xFFE6D7B7.toInt()); oval(c,201f,143f,31f,31f,0xFFE9DCC2.toInt())
            for(i in 0..2) oval(c,(185+i*12).toFloat(),108f,7f,13f,0xFFCB4D45.toInt())
            poly(c,floatArrayOf(226f,142f,251f,151f,226f,159f),0xFFFFB94E.toInt())
            stroke(c,139f,226f,132f,267f,0xFFFFB94E.toInt(),6f); stroke(c,179f,226f,185f,267f,0xFFFFB94E.toInt(),6f)
        }
        oval(c,207f,128f,3f,4f,Color.BLACK)
    }

    private fun drawDragon(c:Canvas) {
        val wing=0xFF8E4C70.toInt(); val scale=0xFF557B8B.toInt()
        poly(c,floatArrayOf(104f,158f,45f,85f,128f,119f,160f,165f,220f,83f,275f,68f,228f,169f),wing)
        for(i in 0..3) stroke(c,(70+i*12).toFloat(),103f,(132+i*9).toFloat(),153f,0x88E8B7C9.toInt(),2f)
        oval(c,154f,186f,72f,43f,scale); oval(c,224f,133f,39f,29f,scale)
        poly(c,floatArrayOf(208f,110f,214f,77f,231f,108f),0xFFFFD078.toInt())
        poly(c,floatArrayOf(230f,107f,253f,78f,248f,117f),0xFFFFD078.toInt())
        stroke(c,105f,191f,58f,238f,scale,12f); stroke(c,58f,238f,32f,217f,scale,8f)
        for(x in listOf(116f,159f,201f,230f)) stroke(c,x,209f,x+5f,265f,scale,12f)
        oval(c,239f,128f,4f,4f,0xFFFFE9B5.toInt()); poly(c,floatArrayOf(250f,145f,277f,153f,249f,159f),0xFF402B38.toInt())
        for(x in listOf(130f,151f,172f,193f)) poly(c,floatArrayOf(x,151f,x+8f,132f,x+15f,155f),0xFFB8D6D0.toInt())
    }

    private fun drawEnvironment(c:Canvas) {
        val v=variant%20
        val skies=intArrayOf(0xFF6EACD0.toInt(),0xFFB5A17E.toInt(),0xFF6D839B.toInt(),0xFF536E91.toInt())
        gradient(c,if(v==18||v==19)0xFF17243E.toInt() else skies[v%4],0xFFDAA879.toInt())
        // distant atmospheric layers
        if(v==5||v==6) {
            for(i in 0..5) poly(c,floatArrayOf(i*64f,210f,(i*64f)+34f,85f,(i*64f)+88f,210f),if(i%2==0)0xFF9C805D.toInt() else 0xFFB8996C.toInt())
            rect(c,0f,210f,320f,320f,0xFFC5A36F.toInt())
            for(i in 0..12) oval(c,(i*31%320).toFloat(),(230+i*17%75).toFloat(),2f,2f,0x66826C4D.toInt())
        } else {
            oval(c,250f,59f,25f,25f,if(v==18)0xFFFFD17A.toInt() else 0xFFFFE3A0.toInt())
            poly(c,floatArrayOf(0f,218f,75f,89f,154f,218f),0xFF516A80.toInt())
            poly(c,floatArrayOf(93f,220f,210f,64f,320f,220f),0xFF40566D.toInt())
            poly(c,floatArrayOf(43f,144f,75f,89f,111f,146f),0xFFE6E9E7.toInt())
            poly(c,floatArrayOf(171f,116f,210f,64f,251f,119f),0xFFE7ECEB.toInt())
            rect(c,0f,210f,320f,320f,if(v==4||v==2)0xFFB9C9D4.toInt() else 0xFF557E59.toInt())
        }
        when(v) {
            0,1,2 -> { // forests with individual trunks and layered canopies
                for(i in 0..7) {
                    val x=18f+i*43f; val h=if(i%2==0)86f else 65f
                    rect(c,x,211f-h/2f,x+12f,275f,0xFF604B38.toInt())
                    oval(c,x+6f,190f-h/2f,25f,35f,if(i%2==0)0xFF28664C.toInt() else 0xFF3C8052.toInt())
                    oval(c,x-5f,197f-h/2f,16f,23f,0x5534A66C)
                }
                if(v==2) { rect(c,0f,0f,320f,320f,0x335D7895.toInt()); for(i in 0..24) stroke(c,(i*17%320).toFloat(),40f,(i*17%320-18).toFloat(),90f,0x88FFFFFF.toInt(),1.4f) }
            }
            3,4 -> { // snow / mountain foreground
                poly(c,floatArrayOf(0f,225f,110f,146f,180f,225f,260f,131f,320f,220f),0xFF9CB6C5.toInt())
                for(i in 0..12) oval(c,(i*27%320).toFloat(),(240+i*11%55).toFloat(),3f,2f,0x99FFFFFF.toInt())
            }
            7 -> { // grassland
                for(i in 0..50) stroke(c,(i*29%320).toFloat(),(255+i*7%55).toFloat(),(i*29%320-4).toFloat(),(240+i*7%55).toFloat(),0xFFB1C96C.toInt(),2f)
            }
            8,9,10,11,12 -> { // water, lake, waterfall, ocean and beach
                rect(c,0f,214f,320f,320f,if(v==12)0xFFD6BC8C.toInt() else 0xFF277EAA.toInt())
                for(i in 0..7) {
                    val y=225f+i*13f
                    stroke(c,12f+(i%2)*24f,y,145f+(i%3)*30f,y-2f,0x88D9F2F5.toInt(),3f)
                    stroke(c,185f,y,302f,y-2f,0x88D9F2F5.toInt(),2f)
                }
                if(v==10) rect(c,136f,90f,180f,223f,0xFFB6D9E3.toInt(),12f)
                if(v==12) oval(c,270f,220f,35f,12f,0xFFEAD5A8.toInt())
            }
            13 -> { // cave mouth and glowing inner rock
                oval(c,160f,208f,132f,125f,0xFF27313C.toInt()); oval(c,160f,220f,90f,87f,0xFF090F19.toInt())
                oval(c,162f,240f,40f,13f,0xFF5A8492.toInt())
                for(i in 0..7) poly(c,floatArrayOf(i*43f,180f,i*43f+19f,125f,i*43f+38f,180f),0xFF66727B.toInt())
            }
            14,15,16 -> { // valley, village and city skyline
                for(i in 0..6) {
                    val x=i*49f
                    rect(c,x,185f,x+31f,244f,if(v==16)0xFF56677A.toInt() else 0xFFB88D65.toInt(),3f)
                    if(v==16) for(y in 0..2) rect(c,x+7f,194f+y*13,x+13f,201f+y*13,0xFFFFD47B.toInt(),1f)
                    else poly(c,floatArrayOf(x-5f,187f,x+15f,168f,x+36f,187f),0xFF815B48.toInt())
                }
            }
            17 -> { // ancient ruins
                for(i in 0..3) { rect(c,35f+i*68f,155f,52f+i*68f,264f,0xFFB89B77.toInt()); poly(c,floatArrayOf(25f+i*68f,158f,43f+i*68f,127f,62f+i*68f,158f),0xFF8F795F.toInt()) }
            }
            18,19 -> { // cinematic night / storm
                for(i in 0..40) oval(c,(i*71%320).toFloat(),(i*37%145).toFloat(),1.5f,1.5f,0xFFFFE8B1.toInt())
                if(v==19) { stroke(c,250f,15f,205f,100f,0x99D9E5F0.toInt(),3f); stroke(c,205f,100f,225f,148f,0x99D9E5F0.toInt(),2f) }
            }
        }
        // Wider cinematic depth: foreground, middle ground and distant landmarks.
        when(v) {
            0,1,2 -> {
                for(i in 0..4) { val x=i*82f-12f; oval(c,x+24f,220f,35f,49f,if(v==2)0xFFB9C9D7.toInt() else 0xFF2E7651.toInt()); rect(c,x+20f,218f,x+28f,305f,0xFF604733.toInt()) }
            }
            3,4,14 -> {
                poly(c,floatArrayOf(0f,258f,48f,180f,105f,251f,160f,151f,232f,249f,282f,185f,320f,255f),if(v==14)0xFF456B4E.toInt() else 0xFF687F91.toInt())
                for(i in 0..7) stroke(c,i*47f,275f,i*47f+20f,266f,0x667E8A91.toInt(),2f)
            }
            5,6 -> {
                for(i in 0..8) { val x=i*42f; oval(c,x+18f,250f,25f,11f,0x66E6C58D.toInt()) }
            }
            7,15,16 -> {
                for(i in 0..10) { val x=i*34f; stroke(c,x,292f,x+9f,260f,0xFF9AB76A.toInt(),2f) }
                if(v==15||v==16) for(i in 0..5) rect(c,i*58f+8f,196f,i*58f+38f,244f,0xFFB68A64.toInt(),3f)
            }
            8,9,10,11,12 -> {
                for(i in 0..10) { val x=i*34f; stroke(c,x,252f,x+18f,246f,0x99D9F5F8.toInt(),2f); stroke(c,x+8f,277f,x+30f,271f,0x77D9F5F8.toInt(),2f) }
                if(v==12) { oval(c,36f,229f,21f,8f,0xFFEAD5A8.toInt()); oval(c,280f,238f,29f,10f,0xFFEAD5A8.toInt()) }
            }
            13 -> { for(i in 0..8) poly(c,floatArrayOf(i*42f,260f,i*42f+20f,220f,i*42f+40f,260f),0xFF59636B.toInt()) }
            17 -> { for(i in 0..5) rect(c,i*57f+5f,232f,i*57f+45f,246f,0xFF8C775E.toInt(),2f) }
            18,19 -> { for(i in 0..35) oval(c,(i*83%320).toFloat(),(i*43%170).toFloat(),1.4f,1.4f,0xFFFFE8B1.toInt()) }
        }
        // atmospheric foreground depth
        rect(c,0f,300f,320f,320f,0x33101B2B.toInt())
    }

    private fun drawArchitecture(c:Canvas) {
        val v=variant%20
        gradient(c,0xFF87B3D0.toInt(),0xFFD5B18B.toInt())
        oval(c,265f,52f,23f,23f,0xFFFFE1A0.toInt())
        rect(c,0f,260f,320f,320f,0xFF637B57.toInt())
        val wall=intArrayOf(0xFFD8BD96.toInt(),0xFFB9C5CB.toInt(),0xFFD6D0C2.toInt(),0xFFB98265.toInt(),0xFF8E9DA9.toInt())[v%5]
        val roof=intArrayOf(0xFF85594A.toInt(),0xFF526A80.toInt(),0xFF8A765F.toInt(),0xFF9A594A.toInt())[v%4]
        when(v) {
            5,6 -> { // palace / castle with towers and battlements
                rect(c,48f,130f,272f,266f,wall,4f)
                rect(c,35f,100f,82f,264f,0xFFBFA17A.toInt(),4f); rect(c,238f,100f,285f,264f,0xFFBFA17A.toInt(),4f)
                poly(c,floatArrayOf(30f,104f,58f,56f,87f,104f),roof); poly(c,floatArrayOf(233f,104f,261f,56f,291f,104f),roof)
                poly(c,floatArrayOf(42f,132f,160f,54f,278f,132f),roof)
                for(x in listOf(95f,125f,195f,225f)) rect(c,x,150f,x+16f,181f,0xFF5B8BA4.toInt(),3f)
                rect(c,137f,202f,183f,266f,0xFF4B3C35.toInt(),4f)
                if(v==6) for(x in listOf(42f,67f,238f,263f)) rect(c,x,83f,x+12f,103f,wall,2f)
            }
            9 -> { // mosque dome and minaret
                rect(c,70f,150f,250f,266f,wall,4f)
                rect(c,35f,95f,58f,264f,0xFFD6C5A4.toInt(),4f)
                poly(c,floatArrayOf(30f,96f,46f,57f,63f,96f),0xFFBFA77D.toInt())
                oval(c,160f,133f,58f,47f,0xFF75A6B7.toInt())
                rect(c,102f,132f,218f,161f,wall,3f)
                rect(c,140f,208f,180f,266f,0xFF705545.toInt(),16f)
                oval(c,160f,208f,20f,18f,0xFF705545.toInt())
                stroke(c,160f,82f,160f,65f,0xFFFFD36C.toInt(),3f)
            }
            15 -> { // bridge
                rect(c,0f,228f,320f,320f,0xFF277DA2.toInt())
                rect(c,0f,185f,320f,207f,0xFFBDA27C.toInt(),4f)
                for(x in listOf(40f,110f,180f,250f)) { rect(c,x,205f,x+13f,290f,0xFF8D765D.toInt()); poly(c,floatArrayOf(x-8f,205f,x+6f,176f,x+21f,205f),0xFF9D896F.toInt()) }
                for(x in 0..10) stroke(c,x*31f,186f,x*31f,160f,0xFFE1CDA8.toInt(),2f)
                stroke(c,0f,160f,320f,160f,0xFFE1CDA8.toInt(),3f)
            }
            17,18,19 -> { // ground tiles and terrain pieces
                rect(c,0f,160f,320f,320f,if(v==17)0xFF56865B.toInt() else if(v==18)0xFFC5A36F.toInt() else 0xFF9AAAB5.toInt())
                for(i in 0..5) for(j in 0..3) rect(c,i*58f+3,j*38f+180f,i*58f+54,j*38f+213f,if(v==17)0xFF6C9B64.toInt() else if(v==18)0xFFD9B985.toInt() else 0xFFCED9E0.toInt(),3f)
            }
            else -> {
                val top=if(v==3||v==4)115f else 132f
                rect(c,66f,top,254f,266f,wall,4f)
                if(v==3) { rect(c,77f,75f,243f,137f,0xFF8295A2.toInt(),3f); rect(c,77f,75f,243f,88f,roof,3f) }
                else if(v==8||v==13||v==14) {
                    rect(c,125f,45f,195f,134f,0xFFB8C5CC.toInt(),4f)
                    rect(c,135f,57f,185f,91f,0xFF77A9C2.toInt(),3f)
                } else poly(c,floatArrayOf(45f,top+5f,160f,47f,275f,top+5f),roof)
                for(x in listOf(88f,196f)) {
                    rect(c,x,155f,x+35f,194f,0xFF77AFC8.toInt(),3f)
                    stroke(c,x+17f,155f,x+17f,194f,0xFFE8F1F3.toInt(),2f)
                }
                rect(c,139f,203f,181f,266f,0xFF654A3C.toInt(),4f)
                oval(c,160f,203f,21f,10f,0xFF8B6650.toInt())
                if(v==0||v==1||v==2) { // domestic roof, chimney and porch
                    rect(c,220f,70f,238f,119f,wall,2f)
                    rect(c,113f,226f,207f,236f,0xFF9D795C.toInt(),2f)
                }
                if(v==10||v==11) { // shop / market awning
                    poly(c,floatArrayOf(62f,151f,258f,151f,244f,177f,76f,177f),0xFFB84E48.toInt())
                    for(x in listOf(88f,125f,162f,199f,236f)) stroke(c,x,153f,x,176f,0xFFFFE4C1.toInt(),2f)
                }
            }
        }
        // facade trim, masonry joints and lit windows
        if(v!=15&&v<17) {
            stroke(c,70f,266f,250f,266f,0x88735E49.toInt(),3f)
            for(y in listOf(137f,198f,257f)) detail(c,75f,y,170f,0x44735E49.toInt())
            if(v==8||v==13) for(x in listOf(103f,217f)) rect(c,x,161f,x+17f,184f,0xFFB6D7E5.toInt(),3f)
        }
        // Wider architectural setting: foreground approach, side wings and site context.
        if(v !in 15..19) {
            poly(c,floatArrayOf(0f,320f,38f,270f,112f,264f,160f,286f,208f,264f,282f,270f,320f,320f),0xFF8C9A79.toInt())
            for(i in 0..5) stroke(c,i*64f,299f,i*64f+28f,286f,0x66E8DFC8.toInt(),2f)
            if(v in 0..4) {
                rect(c,20f,211f,56f,260f,0xFF6D8A63.toInt(),4f)
                oval(c,38f,205f,25f,22f,0xFF3F8054.toInt())
                rect(c,265f,211f,300f,260f,0xFF6D8A63.toInt(),4f)
                oval(c,282f,205f,25f,22f,0xFF3F8054.toInt())
            }
        } else if(v==15) {
            for(i in 0..5) stroke(c,i*60f,233f,i*60f+30f,233f,0x88D9F2F5.toInt(),2f)
        }
        oval(c,160f,283f,105f,10f,0x55000000.toInt())
    }
}
