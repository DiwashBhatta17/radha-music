package com.radha.music;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;

/** Branded progress indicator. No network provider text in loading states. */
public final class RadhaLoadingView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF ring=new RectF();
    private final Drawable logo;
    public RadhaLoadingView(Context context){super(context);logo=context.getDrawable(R.drawable.radha_mark);setContentDescription("Radha Music is loading");paint.setColor(0xFFFF795F);paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);}
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);float size=Math.min(getWidth(),getHeight()),cx=getWidth()/2f,cy=getHeight()/2f;float radius=size*.40f;paint.setStrokeWidth(Math.max(2,size*.045f));ring.set(cx-radius,cy-radius,cx+radius,cy+radius);canvas.drawArc(ring,(android.os.SystemClock.uptimeMillis()%1200)*.3f,260,false,paint);int half=Math.round(size*.21f);logo.setBounds((int)cx-half,(int)cy-half,(int)cx+half,(int)cy+half);logo.draw(canvas);if(isShown())postInvalidateOnAnimation();}
}
