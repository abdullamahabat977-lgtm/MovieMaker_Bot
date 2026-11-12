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
    private val variant: Int,
    private val itemName: String = ""
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
                MovieAssetLibrary.BUILDINGS -> drawArchitecture(c)
                else -> drawProp(c)
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
        // Individual fingers and thumb, with visible knuckles and palm separation.
        for(i in 0..3) {
            val x=78f+i*4.2f
            stroke(c,x,215f,x-1f,224f,skin,2.2f)
            oval(c,x-1f,224f,1.25f,1.8f,0xFFD9A58A.toInt())
        }
        stroke(c,86f,214f,91f,219f,skin,2.4f)
        for(i in 0..3) {
            val x=224f+i*4.2f
            stroke(c,x,215f,x+1f,224f,skin,2.2f)
            oval(c,x+1f,224f,1.25f,1.8f,0xFFD9A58A.toInt())
        }
        stroke(c,234f,214f,229f,219f,skin,2.4f)
        // Separated toes and sole seams on both boots.
        for(i in 0..3) {
            stroke(c,119f+i*6f,270f,119f+i*6f,276f,0xFF9AA5B2.toInt(),1.2f)
            stroke(c,178f+i*6f,270f,178f+i*6f,276f,0xFF9AA5B2.toInt(),1.2f)
        }
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

    private fun drawProp(c:Canvas) {
        val v=variant%20
        val seed=categoryId.hashCode()
        val base=palettes[(v+ kotlin.math.abs(seed%palettes.size))%palettes.size]
        val dark=0xFF263449.toInt()
        gradient(c,0xFF344B66.toInt(),0xFF101A29.toInt())
        // Broad display plinth and soft contact shadow.
        oval(c,160f,276f,112f,15f,0x99000000.toInt())
        rect(c,18f,286f,302f,303f,0x553B526B.toInt(),8f)
        when(categoryId) {
            "furniture" -> drawFurniture(c, v, base)
            "vehicles" -> drawVehicle(c, v, base)
            "weapons" -> drawWeapon(c, v, base)
            "clothes" -> drawClothing(c, v, base)
            "motions" -> {
                // Action pictograms have different body poses, not just a static face.
                oval(c,160f,82f,25f,28f,0xFFD8A17F.toInt())
                when(v%10) {
                    0 -> { stroke(c,160f,112f,160f,196f,base,18f); stroke(c,160f,135f,112f,174f,base,12f); stroke(c,160f,135f,205f,171f,base,12f); stroke(c,160f,196f,125f,263f,dark,13f); stroke(c,160f,196f,198f,263f,dark,13f) }
                    1,2,3 -> { stroke(c,160f,112f,177f,181f,base,18f); stroke(c,166f,130f,111f,151f,base,12f); stroke(c,170f,133f,218f,106f,base,12f); stroke(c,177f,181f,120f,236f,dark,13f); stroke(c,177f,181f,224f,229f,dark,13f) }
                    4,5 -> { stroke(c,160f,112f,160f,180f,base,18f); stroke(c,160f,139f,110f,117f,base,12f); stroke(c,160f,139f,208f,115f,base,12f); stroke(c,160f,180f,121f,240f,dark,13f); stroke(c,160f,180f,198f,239f,dark,13f) }
                    6,7 -> { stroke(c,160f,112f,160f,200f,base,18f); stroke(c,160f,145f,108f,177f,base,12f); stroke(c,160f,145f,211f,177f,base,12f); stroke(c,160f,200f,122f,209f,dark,13f); stroke(c,160f,200f,199f,210f,dark,13f) }
                    else -> { stroke(c,160f,112f,160f,196f,base,18f); stroke(c,160f,145f,112f,93f,base,12f); stroke(c,160f,145f,207f,93f,base,12f); stroke(c,160f,196f,139f,264f,dark,13f); stroke(c,160f,196f,181f,264f,dark,13f) }
                }
                if(v in 8..10) oval(c,220f,85f,19f,19f,0xFFFFD36C.toInt())
            }
            "emotions" -> {
                oval(c,160f,132f,65f,72f,0xFFD8A17F.toInt())
                oval(c,137f,119f,5f,6f,dark); oval(c,183f,119f,5f,6f,dark)
                when(v%10) {
                    0,1,2 -> { stroke(c,132f,104f,146f,99f,dark,4f); stroke(c,174f,99f,188f,104f,dark,4f); stroke(c,139f,158f,181f,158f,0xFF8D4F4A.toInt(),5f) }
                    3,4 -> { stroke(c,133f,111f,148f,119f,dark,4f); stroke(c,172f,119f,187f,111f,dark,4f); stroke(c,141f,166f,179f,166f,0xFF8D4F4A.toInt(),5f); stroke(c,194f,142f,211f,177f,0xFF76B9E8.toInt(),3f) }
                    5,6 -> { stroke(c,132f,108f,148f,102f,dark,5f); stroke(c,172f,102f,188f,108f,dark,5f); stroke(c,142f,162f,178f,151f,0xFF6A3030.toInt(),5f) }
                    7,8 -> { oval(c,160f,151f,10f,14f,0xFF3A2528.toInt()); stroke(c,132f,108f,148f,111f,dark,4f); stroke(c,172f,111f,188f,108f,dark,4f) }
                    else -> { stroke(c,132f,109f,148f,105f,dark,3f); stroke(c,172f,105f,188f,109f,dark,3f); stroke(c,142f,157f,178f,157f,0xFF8D4F4A.toInt(),3f) }
                }
            }
            "cameras" -> {
                rect(c,70f,118f,250f,215f,0xFF27313F.toInt(),15f)
                when(v%8) {
                    0,1 -> { rect(c,105f,88f,215f,122f,0xFF27313F.toInt(),7f); oval(c,160f,165f,57f,57f,0xFF101923.toInt()); oval(c,160f,165f,36f,36f,0xFF5D9AB9.toInt()) }
                    2,3 -> { rect(c,91f,104f,229f,218f,0xFF35465A.toInt(),10f); oval(c,160f,161f,43f,43f,0xFF111923.toInt()); oval(c,160f,161f,25f,25f,0xFF75B6D5.toInt()) }
                    4,5 -> { rect(c,135f,70f,185f,120f,0xFF29384B.toInt(),7f); oval(c,160f,169f,45f,45f,0xFF111923.toInt()); stroke(c,160f,72f,160f,45f,0xFFBFCBD5.toInt(),5f) }
                    else -> { rect(c,83f,133f,237f,197f,0xFF27313F.toInt(),9f); oval(c,160f,165f,30f,30f,0xFF111923.toInt()); stroke(c,160f,133f,160f,82f,metal,5f) }
                }
                rect(c,115f,218f,205f,239f,base,5f)
            }
            "sounds" -> {
                when(v%10) {
                    0,1,2 -> { poly(c,floatArrayOf(80f,145f,130f,145f,194f,95f,194f,225f,130f,176f,80f,176f),base); for(i in 0..2) { val x=210f+i*18f; stroke(c,x,120f-i*12f,x+22f,160f,0xFF9BD4EE.toInt(),4f); stroke(c,x+22f,160f,x,200f+i*12f,0xFF9BD4EE.toInt(),4f) } }
                    3,4 -> { oval(c,160f,160f,63f,63f,base); oval(c,160f,160f,39f,39f,0xFF1C293A.toInt()); oval(c,160f,160f,12f,12f,0xFFE3E9EF.toInt()) }
                    5,6 -> { stroke(c,82f,178f,238f,178f,base,13f); for(i in 0..6) stroke(c,100f+i*21f,178f,100f+i*21f,125f-(i%3)*17f,0xFF9BD4EE.toInt(),4f) }
                    else -> { rect(c,103f,94f,217f,229f,base,13f); oval(c,160f,160f,42f,42f,0xFF25364B.toInt()); for(i in 0..3) stroke(c,125f,115f+i*28f,195f,115f+i*28f,0xFFB7D5E5.toInt(),3f) }
                }
            }
            "phones" -> {
                rect(c,105f,64f,215f,257f,base,22f); rect(c,116f,83f,204f,220f,0xFF15263A.toInt(),8f)
                when(v%5) { 0 -> { oval(c,160f,144f,27f,27f,0xFF6FA7C3.toInt()); oval(c,160f,144f,9f,9f,0xFF172B40.toInt()) }; 1 -> { for(y in listOf(112f,145f,178f)) stroke(c,130f,y,190f,y,0xFF91C6DD.toInt(),4f) }; 2 -> { rect(c,129f,103f,191f,185f,0xFF6FA7C3.toInt(),5f); oval(c,160f,202f,7f,7f,Color.WHITE) }; else -> { oval(c,160f,142f,38f,38f,0xFF6FA7C3.toInt()); stroke(c,135f,196f,185f,196f,Color.WHITE,3f) } }
                oval(c,160f,238f,7f,7f,0xFFE4EAF0.toInt())
            }
            "tools" -> {
                when(v%10) {
                    0 -> { stroke(c,160f,80f,160f,250f,0xFF9C9FA4.toInt(),12f); poly(c,floatArrayOf(160f,72f,105f,112f,215f,112f),base) }
                    1 -> { stroke(c,115f,105f,210f,220f,0xFF9C9FA4.toInt(),13f); oval(c,110f,100f,19f,19f,base); rect(c,195f,210f,226f,237f,0xFF8C5B3E.toInt(),5f) }
                    2 -> { rect(c,105f,80f,215f,238f,base,8f); rect(c,127f,103f,193f,126f,0xFF29384B.toInt(),5f); for(y in listOf(148f,177f,206f)) stroke(c,130f,y,190f,y,Color.WHITE,3f) }
                    3 -> { poly(c,floatArrayOf(100f,95f,220f,95f,195f,245f,125f,245f),base); stroke(c,125f,105f,195f,105f,0xFFB8C6D2.toInt(),5f) }
                    else -> { stroke(c,95f,210f,225f,110f,base,17f); stroke(c,180f,80f,245f,145f,0xFF9C9FA4.toInt(),10f); oval(c,95f,210f,15f,15f,0xFFB8C6D2.toInt()) }
                }
            }
            "medical" -> {
                when(v%8) {
                    0 -> { rect(c,91f,135f,229f,190f,0xFFE6E9E8.toInt(),9f); rect(c,145f,112f,175f,213f,0xFFC84D54.toInt(),4f); rect(c,119f,149f,201f,177f,0xFFC84D54.toInt(),4f) }
                    1 -> { stroke(c,160f,84f,160f,240f,0xFFD5DFE8.toInt(),5f); oval(c,160f,154f,35f,35f,0xFFE6E9E8.toInt()) }
                    2 -> { rect(c,105f,95f,215f,235f,0xFFE6E9E8.toInt(),8f); oval(c,160f,155f,35f,35f,0xFFC84D54.toInt()) }
                    3 -> { stroke(c,160f,82f,160f,230f,0xFFBFCBD5.toInt(),9f); oval(c,160f,90f,18f,18f,0xFFE6E9E8.toInt()); oval(c,160f,230f,18f,18f,0xFFE6E9E8.toInt()) }
                    else -> { rect(c,96f,106f,224f,222f,0xFFE6E9E8.toInt(),14f); for(i in 0..3) rect(c,112f+i*27f,137f,132f+i*27f,191f,0xFFC84D54.toInt(),3f) }
                }
            }
            "food" -> {
                when(v%8) {
                    0 -> { oval(c,160f,184f,76f,43f,0xFFE9D8B8.toInt()); oval(c,160f,174f,58f,29f,0xFFB97B46.toInt()); stroke(c,160f,120f,160f,151f,0xFF4F8A50.toInt(),5f) }
                    1 -> { oval(c,160f,165f,57f,65f,0xFFCB4D45.toInt()); rect(c,154f,90f,166f,112f,0xFF4F8A50.toInt(),5f) }
                    2 -> { oval(c,160f,165f,45f,68f,0xFFECCB55.toInt()); oval(c,160f,99f,11f,10f,0xFF6D9A4F.toInt()) }
                    3 -> { oval(c,160f,170f,72f,47f,0xFF8B5A38.toInt()); for(i in 0..5) oval(c,120f+i*16f,152f,6f,6f,0xFFFFD36C.toInt()) }
                    4 -> { rect(c,98f,130f,222f,205f,0xFFE7E0D2.toInt(),10f); oval(c,160f,166f,36f,27f,0xFFB97B46.toInt()) }
                    else -> { oval(c,160f,175f,68f,48f,0xFFE9D8B8.toInt()); for(i in 0..5) oval(c,117f+i*17f,164f,8f,12f,0xFF6A9B76.toInt()) }
                }
            }
            "office" -> {
                when(v%8) { 0 -> { rect(c,69f,95f,251f,220f,0xFF283444.toInt(),10f); rect(c,82f,109f,238f,196f,0xFF6FA7C3.toInt(),5f); rect(c,145f,220f,175f,248f,0xFF8E9BA7.toInt(),4f) }; 1 -> { rect(c,94f,75f,226f,255f,0xFFE6E9E8.toInt(),6f); for(y in listOf(112f,154f,196f)) stroke(c,108f,y,212f,y,0xFF718090.toInt(),3f) }; 2 -> { rect(c,90f,105f,230f,220f,0xFF6A4E3C.toInt(),5f); poly(c,floatArrayOf(90f,105f,160f,58f,230f,105f),0xFFB98A58.toInt()) }; else -> { rect(c,85f,110f,235f,220f,base,7f); oval(c,160f,105f,20f,20f,0xFFFFD36C.toInt()); for(i in 0..4) stroke(c,105f+i*27f,135f,105f+i*27f,205f,0x66FFFFFF.toInt(),2f) } }
            }
            "electronics" -> {
                when(v%8) { 0 -> { rect(c,69f,95f,251f,220f,0xFF283444.toInt(),10f); rect(c,82f,109f,238f,196f,0xFF6FA7C3.toInt(),5f); rect(c,145f,220f,175f,248f,0xFF8E9BA7.toInt(),4f) }; 1 -> { rect(c,119f,67f,201f,256f,0xFF333D4B.toInt(),15f); oval(c,160f,234f,6f,6f,Color.WHITE) }; 2 -> { rect(c,72f,125f,248f,210f,0xFF3B4654.toInt(),8f); rect(c,98f,93f,222f,128f,0xFF8A9BA9.toInt(),6f); oval(c,160f,168f,20f,20f,0xFF6FA7C3.toInt()) }; else -> { rect(c,91f,95f,229f,228f,base,9f); for(i in 0..5) oval(c,112f+(i%3)*48f,125f+(i/3)*44f,9f,9f,0xFFB8C6D2.toInt()) } }
            }
            "sports" -> {
                when(v%8) { 0 -> { oval(c,160f,160f,68f,68f,0xFFE8E9E4.toInt()); for(i in 0..4) stroke(c,112f+i*22f,110f,208f-i*22f,210f,0xFF303B4A.toInt(),3f) }; 1 -> { oval(c,160f,160f,65f,65f,0xFFFFA34A.toInt()); stroke(c,160f,95f,160f,225f,Color.WHITE,4f) }; 2 -> { rect(c,102f,82f,218f,239f,0xFFB77C48.toInt(),8f); stroke(c,160f,83f,160f,239f,0xFF5B3B28.toInt(),3f) }; 3 -> { stroke(c,100f,110f,220f,210f,0xFFB8C6D2.toInt(),12f); oval(c,100f,110f,14f,14f,base); oval(c,220f,210f,14f,14f,base) }; else -> { oval(c,160f,160f,53f,53f,base); for(i in 0..5) stroke(c,120f+i*16f,112f,120f+i*16f,208f,Color.WHITE,2f) } }
            }
            "travel" -> {
                when(v%8) { 0 -> { rect(c,91f,93f,229f,250f,base,18f); rect(c,126f,73f,194f,108f,0xFFB8C6D2.toInt(),9f); rect(c,107f,123f,213f,190f,0x55303D50.toInt(),9f) }; 1 -> { rect(c,120f,80f,200f,250f,0xFFB9A27B.toInt(),7f); stroke(c,120f,115f,200f,115f,0xFF735A3E.toInt(),5f); rect(c,140f,250f,180f,265f,dark,4f) }; 2 -> { oval(c,160f,164f,69f,69f,base); stroke(c,160f,95f,160f,233f,0xFFB8C6D2.toInt(),4f) }; 3 -> { poly(c,floatArrayOf(160f,70f,225f,150f,195f,252f,125f,252f,95f,150f),base); stroke(c,160f,72f,160f,247f,0xFFB8C6D2.toInt(),4f) }; else -> { rect(c,86f,126f,234f,217f,base,8f); stroke(c,110f,126f,110f,217f,0xFFB8C6D2.toInt(),4f); stroke(c,210f,126f,210f,217f,0xFFB8C6D2.toInt(),4f) } }
            }
            "road" -> {
                when(v%8) { 0 -> { rect(c,145f,83f,175f,259f,0xFF8E9BA7.toInt(),5f); poly(c,floatArrayOf(85f,95f,235f,95f,220f,190f,100f,190f),0xFFB94E4E.toInt()) }; 1 -> { rect(c,80f,85f,240f,245f,0xFF8E9BA7.toInt(),5f); for(i in 0..3) rect(c,95f,105f+i*32f,225f,111f+i*32f,0xFFFFD36C.toInt(),2f) }; 2 -> { oval(c,160f,155f,74f,74f,0xFFB94E4E.toInt()); oval(c,160f,155f,57f,57f,Color.WHITE); stroke(c,160f,94f,160f,216f,dark,4f) }; else -> { poly(c,floatArrayOf(160f,70f,238f,215f,82f,215f),0xFFFFD36C.toInt()); stroke(c,160f,113f,160f,172f,dark,7f); oval(c,160f,193f,5f,5f,dark) } }
            }
            else -> {
                when(v%8) { 0 -> { rect(c,83f,113f,237f,236f,base,12f); oval(c,160f,104f,31f,31f,0xFFFFD36C.toInt()) }; 1 -> { oval(c,160f,160f,70f,70f,base); oval(c,160f,160f,38f,38f,0xFFB8C6D2.toInt()) }; 2 -> { poly(c,floatArrayOf(160f,67f,236f,126f,207f,245f,113f,245f,84f,126f),base) }; else -> { rect(c,91f,95f,229f,228f,base,9f); for(i in 0..5) stroke(c,112f+i*19f,110f,112f+i*19f,210f,0x66FFFFFF.toInt(),2f) } }
            }
        }
        // Distinct colorway, panel seams, highlights and a unique index marker.
        for(i in 0..(v%5)) stroke(c,80f,246f+i*5f,240f,246f+i*5f,0x33FFFFFF.toInt(),1.4f)
        oval(c,160f,279f,4f,4f,0xFFFFD36C.toInt())
    }


    // Each catalogue entry gets its own recognizable silhouette, not a shared placeholder.
    private fun drawWeapon(c: Canvas, v: Int, base: Int) {
        val metal=0xFFD5DFE8.toInt()
        val wood=0xFF875B3E.toInt()
        // Every index maps to a different non-functional theatrical prop silhouette.
        when(v) {
            0 -> { // sword
                poly(c,floatArrayOf(160f,35f,174f,126f,165f,194f,155f,194f,146f,126f),metal)
                stroke(c,160f,190f,160f,246f,wood,14f); stroke(c,125f,190f,195f,190f,0xFFC89A55.toInt(),9f)
            }
            1 -> { // dagger
                poly(c,floatArrayOf(160f,79f,174f,139f,163f,184f,157f,184f,146f,139f),0xFFBFCBD5.toInt())
                stroke(c,160f,181f,160f,232f,wood,12f); stroke(c,137f,190f,183f,190f,0xFFC89A55.toInt(),7f)
            }
            2 -> { // spear
                stroke(c,160f,69f,160f,274f,wood,10f)
                poly(c,floatArrayOf(160f,28f,142f,91f,160f,78f,178f,91f),metal)
            }
            3 -> { // arrow bundle
                for(i in 0..4) { val x=112f+i*23f; stroke(c,x,90f,x,242f,wood,3f); poly(c,floatArrayOf(x,68f,x-7f,94f,x+7f,94f),metal); poly(c,floatArrayOf(x,242f,x-7f,230f,x+7f,230f),0xFFB64E4E.toInt()) }
            }
            4 -> { // bow
                val path=Path().apply{moveTo(160f,55f);quadTo(45f,160f,160f,265f)}
                fill(c,wood);p.style=Paint.Style.STROKE;p.strokeWidth=12f;c.drawPath(path,p);p.style=Paint.Style.FILL
                stroke(c,160f,55f,160f,265f,metal,2f)
            }
            5,6 -> { // round wooden / metal shield
                val col=if(v==5)wood else 0xFF8796A5.toInt()
                oval(c,160f,163f,83f,104f,col);oval(c,160f,163f,68f,88f,base)
                oval(c,160f,163f,17f,17f,0xFFFFD36C.toInt())
                for(i in 0..7) { val y=88f+i*21f; stroke(c,105f,y,215f,y,0x55FFFFFF.toInt(),2f) }
            }
            7 -> { // mace
                stroke(c,160f,135f,160f,270f,wood,13f); oval(c,160f,94f,34f,35f,metal)
                for(i in 0..7) { val a=i*45f; val x=160f+kotlin.math.cos(Math.toRadians(a.toDouble())).toFloat()*39f; val y=94f+kotlin.math.sin(Math.toRadians(a.toDouble())).toFloat()*39f; oval(c,x,y,8f,8f,0xFF8796A5.toInt()) }
            }
            8 -> { // fantasy staff
                stroke(c,160f,90f,160f,270f,wood,12f); oval(c,160f,67f,31f,31f,0xFF9D83D7.toInt()); oval(c,160f,67f,15f,15f,0xFFE5D9FF.toInt())
                for(i in 0..5) stroke(c,160f,98f,95f+i*26f,43f,0x887FBAFF.toInt(),3f)
            }
            9 -> { // pistol-shaped stage prop
                rect(c,84f,124f,234f,156f,0xFF3A4654.toInt(),8f); rect(c,179f,147f,210f,222f,wood,7f)
                rect(c,98f,110f,222f,129f,metal,4f); rect(c,218f,129f,247f,150f,0xFF26313E.toInt(),4f)
                stroke(c,125f,156f,125f,174f,metal,3f)
            }
            10 -> { // AK-style movie prop
                rect(c,51f,128f,253f,151f,0xFF343F4A.toInt(),5f); rect(c,183f,113f,253f,126f,metal,3f)
                poly(c,floatArrayOf(56f,130f,111f,130f,101f,177f,47f,170f),wood)
                rect(c,142f,149f,163f,218f,wood,5f); rect(c,108f,116f,172f,127f,0xFF8D989F.toInt(),3f)
                rect(c,246f,132f,270f,145f,metal,3f)
            }
            11 -> { // long rifle stage prop
                rect(c,36f,131f,278f,147f,0xFF3C4854.toInt(),5f); rect(c,210f,118f,283f,129f,metal,3f)
                poly(c,floatArrayOf(39f,132f,111f,132f,99f,171f,34f,165f),wood)
                rect(c,145f,147f,160f,207f,wood,4f); rect(c,89f,118f,126f,131f,0xFF9DAAB4.toInt(),3f)
            }
            12 -> { // double-barrel hunting prop
                rect(c,42f,133f,266f,153f,wood,5f); rect(c,151f,108f,282f,121f,metal,4f)
                rect(c,151f,125f,282f,138f,metal,4f); poly(c,floatArrayOf(44f,136f,112f,136f,100f,181f,39f,172f),0xFF76523A.toInt())
                rect(c,137f,151f,156f,194f,wood,4f)
            }
            13 -> { // mounted heavy theatrical prop
                rect(c,44f,117f,278f,163f,0xFF374451.toInt(),8f); rect(c,218f,102f,288f,120f,metal,4f)
                rect(c,110f,158f,171f,208f,wood,5f); rect(c,67f,209f,213f,224f,0xFF697989.toInt(),5f)
                for(x in listOf(99f,143f,187f)) oval(c,x,225f,9f,9f,metal)
            }
            14 -> { // old cannon
                rect(c,57f,137f,246f,169f,0xFF485564.toInt(),10f); rect(c,189f,108f,287f,139f,0xFF596777.toInt(),8f)
                oval(c,105f,207f,32f,32f,0xFF26313D.toInt());oval(c,219f,207f,32f,32f,0xFF26313D.toInt())
                stroke(c,158f,168f,158f,224f,wood,12f);stroke(c,74f,239f,250f,239f,0xFF7D8995.toInt(),7f)
            }
            15 -> { // helmet
                oval(c,160f,123f,78f,62f,metal); rect(c,82f,118f,238f,143f,0xFF8999A8.toInt(),8f)
                stroke(c,160f,50f,160f,91f,0xFF6E7D8D.toInt(),5f)
            }
            16 -> { // chest armour
                poly(c,floatArrayOf(92f,89f,142f,103f,160f,91f,178f,103f,228f,89f,251f,238f,160f,276f,69f,238f),metal)
                stroke(c,160f,106f,160f,246f,0xFF8797A6.toInt(),4f)
            }
            17 -> { // arm guards
                rect(c,94f,105f,139f,250f,metal,15f);rect(c,181f,105f,226f,250f,metal,15f)
                for(y in listOf(132f,180f,228f)){oval(c,116f,y,7f,7f,0xFF758493.toInt());oval(c,204f,y,7f,7f,0xFF758493.toInt())}
            }
            18 -> { // leg guards
                poly(c,floatArrayOf(91f,76f,146f,76f,151f,263f,104f,263f),metal)
                poly(c,floatArrayOf(174f,76f,229f,76f,216f,263f,169f,263f),metal)
                for(y in listOf(125f,183f,235f)) stroke(c,101f,y,145f,y,0xFF758493.toInt(),4f)
            }
            else -> { // fantasy battle axe
                stroke(c,160f,110f,160f,270f,wood,13f)
                poly(c,floatArrayOf(160f,112f,100f,72f,112f,147f,160f,170f,208f,147f,220f,72f),metal)
                stroke(c,160f,121f,160f,161f,0xFF8796A5.toInt(),3f)
            }
        }
    }

    private fun drawVehicle(c: Canvas, v: Int, base: Int) {
        val glass=0xFF91C4DA.toInt(); val tire=0xFF17202B.toInt()
        fun wheel(x:Float,y:Float,r:Float=22f){oval(c,x,y,r,r,tire);oval(c,x,y,r*0.43f,r*0.43f,0xFFB9C4CE.toInt())}
        when(v) {
            0,1,2,3,4 -> {
                val top=if(v==2)145f else if(v==3)135f else 153f
                val body=if(v==2)0xFFB83E45.toInt() else if(v==3)0xFF73513F.toInt() else base
                rect(c,58f,top,262f,226f,body,if(v==3)5f else 18f)
                poly(c,floatArrayOf(91f,top,122f,91f,207f,91f,235f,top),body)
                poly(c,floatArrayOf(126f,top-8f,198f,top-8f,218f,top+3f,106f,top+3f),glass)
                wheel(103f,225f);wheel(220f,225f)
                if(v==2){rect(c,72f,163f,248f,174f,0xFF202A35.toInt(),3f);rect(c,88f,185f,232f,194f,0xFFFFD36C.toInt(),3f)}
                if(v==3){rect(c,77f,144f,243f,157f,0xFFB18A59.toInt(),3f);rect(c,150f,130f,170f,221f,0xFFB18A59.toInt(),2f)}
                if(v==4){rect(c,88f,162f,232f,177f,0xFF5B9EAE.toInt(),4f);oval(c,160f,188f,12f,12f,0xFFB9E4F0.toInt())}
            }
            5 -> { rect(c,45f,133f,275f,225f,0xFF64765B.toInt(),12f); poly(c,floatArrayOf(88f,133f,118f,91f,211f,91f,238f,133f),0xFF64765B.toInt()); poly(c,floatArrayOf(125f,100f,204f,100f,222f,132f,105f,132f),glass); wheel(91f,226f,25f);wheel(231f,226f,25f);rect(c,45f,170f,275f,180f,0xFF3F4D3D.toInt(),4f) }
            6 -> { rect(c,41f,151f,277f,224f,base,9f); rect(c,55f,103f,145f,157f,glass,7f); rect(c,157f,121f,273f,184f,0xFF9B795B.toInt(),4f); wheel(92f,225f);wheel(231f,225f); rect(c,163f,132f,267f,142f,0xFFB6C1C8.toInt(),2f) }
            7,8,9 -> { rect(c,57f,118f,263f,226f,if(v==7)Color.WHITE else if(v==8)0xFF263D5A.toInt() else 0xFFE34C3F.toInt(),12f); rect(c,88f,78f,231f,146f,glass,9f); wheel(101f,226f);wheel(220f,226f); if(v==7){rect(c,145f,151f,175f,194f,0xFFC84D54.toInt(),4f);rect(c,155f,158f,165f,187f,Color.WHITE,2f)}; if(v==8){rect(c,70f,126f,250f,140f,Color.WHITE,3f);oval(c,160f,133f,12f,12f,0xFF3B74B9.toInt())}; if(v==9){rect(c,80f,101f,240f,114f,0xFFFFD36C.toInt(),4f);rect(c,72f,157f,248f,168f,0xFFFFD36C.toInt(),4f);stroke(c,240f,120f,265f,64f,0xFFE34C3F.toInt(),7f)} }
            10,11 -> { rect(c,57f,77f,263f,226f,if(v==11)0xFFF0B93F.toInt() else base,9f); rect(c,77f,91f,243f,144f,glass,5f); for(x in listOf(88f,132f,176f,220f)) rect(c,x,151f,x+29f,188f,if(v==11)0xFF34485C.toInt() else 0xFF6FA7C3.toInt(),3f); wheel(100f,226f);wheel(220f,226f); if(v==11){rect(c,61f,80f,259f,91f,0xFF34485C.toInt(),3f);rect(c,61f,203f,259f,214f,0xFF34485C.toInt(),3f)} }
            12,13 -> { rect(c,40f,132f,280f,226f,if(v==12)0xFF697C89.toInt() else 0xFFB58B5B.toInt(),6f); rect(c,47f,82f,137f,151f,glass,5f); if(v==12){rect(c,148f,102f,273f,212f,0xFF71818C.toInt(),5f);for(y in listOf(116f,150f,184f))stroke(c,157f,y,266f,y,0xFF9EADB7.toInt(),2f)}else{rect(c,149f,107f,271f,205f,0xFF9C7048.toInt(),4f);for(x in listOf(170f,210f,250f))rect(c,x,118f,x+25f,192f,0xFFBF9668.toInt(),2f)};wheel(91f,226f,24f);wheel(232f,226f,24f)}
            14,15,16 -> { val r=if(v==16)18f else 22f; wheel(83f,224f,r);wheel(237f,224f,r); stroke(c,83f,224f,145f,153f,base,7f);stroke(c,145f,153f,237f,224f,base,7f);stroke(c,145f,153f,175f,224f,base,6f); if(v==14){oval(c,154f,137f,26f,19f,base);rect(c,135f,171f,188f,185f,0xFF303B4A.toInt(),5f)}else if(v==15){oval(c,160f,154f,38f,18f,base);rect(c,137f,167f,183f,184f,0xFF303B4A.toInt(),6f);stroke(c,145f,151f,126f,118f,0xFFD4DEE5.toInt(),4f)}else{stroke(c,145f,153f,127f,114f,0xFFD4DEE5.toInt(),4f);stroke(c,127f,114f,110f,114f,0xFFD4DEE5.toInt(),4f);stroke(c,145f,153f,145f,224f,0xFFD4DEE5.toInt(),3f)}}
            17 -> { oval(c,158f,174f,70f,38f,base);rect(c,119f,150f,178f,178f,glass,12f);stroke(c,158f,139f,158f,79f,0xFFBFCBD5.toInt(),7f);stroke(c,55f,79f,265f,79f,0xFFBFCBD5.toInt(),5f);stroke(c,118f,211f,220f,211f,0xFF7D8996.toInt(),5f);stroke(c,210f,174f,269f,140f,base,9f);oval(c,272f,140f,12f,12f,0xFFBFCBD5.toInt())}
            18 -> { poly(c,floatArrayOf(160f,35f,179f,140f,285f,201f,285f,220f,177f,187f,169f,272f,151f,272f,143f,187f,35f,220f,35f,201f,141f,140f),base);rect(c,151f,84f,169f,190f,glass,6f);for(x in listOf(105f,215f))oval(c,x,202f,10f,10f,0xFF34485C.toInt()) }
            else -> { poly(c,floatArrayOf(39f,205f,281f,205f,239f,261f,81f,261f),base);rect(c,132f,112f,205f,205f,0xFFE2E7E9.toInt(),5f);poly(c,floatArrayOf(136f,116f,136f,52f,190f,116f),0xFFE8D9B7.toInt());stroke(c,20f,272f,300f,272f,0xFF63B6D5.toInt(),4f)}
        }
    }

    private fun drawFurniture(c: Canvas, v: Int, base: Int) {
        val wood=0xFF9A704D.toInt()
        when(v) {
            0 -> { rect(c,92f,130f,228f,153f,base,7f); rect(c,103f,77f,119f,139f,wood,4f); rect(c,201f,77f,217f,139f,wood,4f); rect(c,103f,151f,117f,267f,wood,4f); rect(c,203f,151f,217f,267f,wood,4f) }
            1 -> { rect(c,77f,127f,243f,151f,base,13f); rect(c,88f,76f,105f,139f,0xFF647B91.toInt(),7f); rect(c,215f,76f,232f,139f,0xFF647B91.toInt(),7f); for(x in listOf(94f,219f)) rect(c,x,151f,x+10f,267f,0xFF647B91.toInt(),4f) }
            2 -> { rect(c,75f,120f,245f,145f,base,10f); rect(c,82f,82f,238f,130f,0xFF27384B.toInt(),9f); rect(c,84f,146f,98f,258f,wood,4f); rect(c,222f,146f,236f,258f,wood,4f); oval(c,160f,105f,9f,9f,0xFFFFD36C.toInt()) }
            3,4,5 -> { val width=if(v==3)150f else if(v==4)210f else 118f; val l=160f-width/2f; val r=160f+width/2f; rect(c,l,158f,r,218f,base,17f); rect(c,l+8f,96f,r-8f,169f,base,16f); rect(c,l+8f,210f,l+20f,269f,wood,4f); rect(c,r-20f,210f,r-8f,269f,wood,4f); if(v==5){rect(c,l,155f,l+17f,219f,wood,6f);rect(c,r-17f,155f,r,219f,wood,6f)} }
            6 -> { rect(c,52f,155f,268f,208f,wood,5f); rect(c,65f,112f,255f,161f,0xFFE6D9C7.toInt(),7f); rect(c,63f,205f,78f,271f,wood,4f); rect(c,247f,205f,262f,271f,wood,4f); rect(c,85f,138f,235f,146f,base,4f) }
            7 -> { rect(c,77f,161f,243f,207f,wood,5f); rect(c,86f,125f,234f,166f,0xFFF0DCC7.toInt(),8f); rect(c,91f,205f,103f,266f,wood,4f); rect(c,217f,205f,229f,266f,wood,4f); oval(c,160f,148f,17f,10f,0xFFFFD36C.toInt()) }
            8 -> { rect(c,65f,164f,255f,191f,wood,4f); rect(c,73f,80f,247f,169f,base,5f); rect(c,73f,205f,87f,268f,wood,4f); rect(c,233f,205f,247f,268f,wood,4f); rect(c,73f,62f,247f,83f,wood,4f); rect(c,73f,190f,247f,210f,wood,4f) }
            9 -> { rect(c,54f,133f,266f,153f,wood,5f); for(x in listOf(68f,238f)) rect(c,x,153f,x+14f,264f,wood,4f); oval(c,160f,129f,34f,10f,base) }
            10 -> { rect(c,48f,130f,272f,153f,wood,4f); rect(c,60f,153f,74f,265f,wood,4f); rect(c,246f,153f,260f,265f,wood,4f); rect(c,95f,154f,225f,202f,0xFF70513A.toInt(),4f); rect(c,101f,160f,219f,196f,base,3f); rect(c,122f,205f,198f,212f,wood,2f) }
            11 -> { rect(c,83f,157f,237f,176f,wood,5f); rect(c,95f,176f,108f,245f,wood,4f); rect(c,212f,176f,225f,245f,wood,4f); oval(c,160f,147f,31f,14f,base) }
            12 -> { rect(c,57f,124f,263f,146f,wood,4f); rect(c,69f,146f,83f,266f,wood,4f); rect(c,237f,146f,251f,266f,wood,4f); rect(c,105f,146f,215f,207f,base,4f); rect(c,112f,155f,208f,196f,0xFF6C91A7.toInt(),3f) }
            13 -> { rect(c,82f,65f,238f,264f,base,4f); for(y in listOf(80f,145f,210f)) rect(c,87f,y,233f,y+5f,wood,2f); rect(c,100f,92f,145f,137f,0xFF7A91A4.toInt(),3f); rect(c,175f,92f,220f,137f,0xFF7A91A4.toInt(),3f); rect(c,100f,157f,220f,201f,0xFF7A91A4.toInt(),3f) }
            14 -> { rect(c,72f,65f,248f,266f,wood,3f); for(y in listOf(108f,158f,208f)) { rect(c,79f,y,241f,y+5f,0xFFD7C2A5.toInt(),2f); for(x in listOf(91f,135f,179f,223f)) rect(c,x,y+6f,x+34f,y+40f,base,2f) } }
            15 -> { rect(c,85f,58f,235f,267f,base,5f); rect(c,96f,70f,224f,255f,0xFF70513A.toInt(),4f); rect(c,104f,82f,216f,245f,0xFFB98C65.toInt(),4f); rect(c,154f,82f,166f,245f,wood,3f); oval(c,145f,165f,4f,4f,0xFFFFD36C.toInt()) }
            16 -> { rect(c,62f,111f,258f,263f,base,4f); rect(c,76f,127f,244f,252f,0xFFB98C65.toInt(),3f); rect(c,90f,85f,230f,113f,wood,4f); rect(c,108f,153f,212f,225f,0xFF7A91A4.toInt(),3f); for(x in listOf(91f,229f)) rect(c,x,145f,x+7f,236f,wood,2f) }
            17 -> { rect(c,55f,168f,265f,206f,wood,5f); rect(c,77f,92f,243f,171f,0xFF27313F.toInt(),8f); rect(c,88f,103f,232f,158f,0xFF5D9AB9.toInt(),4f); rect(c,105f,208f,117f,255f,wood,3f); rect(c,203f,208f,215f,255f,wood,3f) }
            18 -> { stroke(c,160f,119f,160f,263f,wood,9f); oval(c,160f,82f,42f,34f,0xFFFFE2A0.toInt()); poly(c,floatArrayOf(118f,82f,202f,82f,190f,110f,130f,110f),0xFFFFD17A.toInt()); rect(c,126f,258f,194f,270f,wood,4f) }
            else -> { rect(c,39f,103f,281f,258f,0xFFB58D63.toInt(),8f); for(i in 0..8) stroke(c,52f+i*27f,112f,52f+i*27f,249f,0xFF8C684B.toInt(),2f); for(y in listOf(120f,160f,200f,240f)) stroke(c,47f,y,273f,y,0x66F1D8B5.toInt(),2f) }
        }
    }

    private fun drawClothing(c: Canvas, v: Int, base: Int) {
        when(v) {
            0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15 -> {
                val long=when(v){1,3,9,10,14,15->true;else->false}
                val col=when(v){4->0xFF53694B.toInt();5->0xFF34485F.toInt();6,7->0xFFE8E9E2.toInt();8->0xFF805F42.toInt();9->0xFF34485F.toInt();10->0xFF607E9A.toInt();11->0xFF4F9A9A.toInt();12->0xFF303A4B.toInt();13->0xFF9B744C.toInt();14->0xFF67435C.toInt();15->0xFF8B4F72.toInt();else->base}
                if(long) poly(c,floatArrayOf(126f,83f,194f,83f,242f,270f,78f,270f),col)
                else poly(c,floatArrayOf(117f,91f,145f,112f,175f,112f,203f,91f,244f,130f,213f,166f,203f,236f,117f,236f,107f,166f,76f,130f),col)
                stroke(c,160f,111f,160f,245f,0x66FFFFFF.toInt(),3f)
                if(v==3||v==13) { rect(c,119f,137f,201f,177f,0xFFB58C5B.toInt(),4f);rect(c,119f,180f,201f,190f,0xFF654A3C.toInt(),2f) }
                if(v==4||v==5) { rect(c,119f,137f,201f,226f,0x55303B45.toInt(),4f);for(x in listOf(130f,190f))rect(c,x,151f,x+14f,181f,0xFFB9A27B.toInt(),2f) }
                if(v==6||v==7) { rect(c,146f,116f,174f,132f,0xFFC84D54.toInt(),3f);rect(c,155f,120f,165f,128f,Color.WHITE,1f) }
                if(v==9) { poly(c,floatArrayOf(113f,91f,207f,91f,227f,128f,93f,128f),0xFF536B85.toInt());stroke(c,160f,130f,160f,257f,0xFFB8C6D2.toInt(),3f) }
                if(v==10) { stroke(c,84f,125f,236f,125f,0xFFB8C6D2.toInt(),5f);for(i in 0..4)stroke(c,100f+i*30f,135f,100f+i*30f,235f,0x66FFFFFF.toInt(),2f) }
                if(v==11) { stroke(c,120f,180f,200f,180f,0xFFFFD36C.toInt(),4f);rect(c,120f,195f,200f,220f,0xFF34485C.toInt(),4f) }
                if(v==12) { rect(c,118f,205f,202f,240f,0xFF29384B.toInt(),3f);stroke(c,160f,205f,160f,240f,0xFFB8C6D2.toInt(),2f) }
                if(v==14||v==15) { rect(c,118f,90f,202f,105f,0xFFFFD36C.toInt(),3f);for(i in 0..4)oval(c,130f+i*15f,210f,3f,4f,0xFFFFD36C.toInt()) }
            }
            16 -> { oval(c,160f,93f,68f,35f,base);rect(c,94f,89f,226f,113f,0xFF4A3C31.toInt(),5f);rect(c,153f,56f,167f,88f,0xFF4A3C31.toInt(),3f) }
            17 -> { oval(c,160f,99f,71f,31f,base);poly(c,floatArrayOf(92f,98f,228f,98f,244f,112f,76f,112f),0xFF4A3C31.toInt()) }
            18,19 -> { poly(c,floatArrayOf(95f,124f,225f,124f,244f,258f,76f,258f),base);rect(c,104f,238f,216f,266f,0xFF253143.toInt(),9f);stroke(c,160f,145f,160f,238f,0xFFE4E8EA.toInt(),3f);for(i in 0..4)stroke(c,124f+i*18f,252f,124f+i*18f,261f,0xFF9CAAB5.toInt(),2f);if(v==19)stroke(c,104f,174f,216f,174f,0xFFFFD36C.toInt(),3f)}
        }
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
