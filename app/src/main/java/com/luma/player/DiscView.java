package com.radha.music;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** A real circular disc: artwork, fine concentric grooves and a transparent-looking hub. */
public final class DiscView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final ValueAnimator spin=ValueAnimator.ofFloat(0,360);
    private Bitmap artwork;
    private float angle;
    public DiscView(Context c){super(c);setContentDescription("Album disc. Swipe left for next song, right for previous song.");spin.setDuration(20000);spin.setRepeatCount(ValueAnimator.INFINITE);spin.setInterpolator(new LinearInterpolator());spin.addUpdateListener(a->{angle=(float)a.getAnimatedValue();invalidate();});}
    public void setArtwork(Bitmap b){artwork=b;invalidate();}
    public void setPlaying(boolean playing){if(playing){if(spin.isPaused())spin.resume();else if(!spin.isStarted())spin.start();}else if(spin.isStarted()&&!spin.isPaused())spin.pause();}
    @Override protected void onDetachedFromWindow(){spin.cancel();super.onDetachedFromWindow();}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float x=getWidth()/2f,y=getHeight()/2f,r=Math.min(x,y)*.9f;
        paint.setShader(new RadialGradient(x,y,r*1.12f,new int[]{0x444F45B9,0x004F45B9},null,Shader.TileMode.CLAMP));c.drawCircle(x,y,r*1.12f,paint);paint.setShader(null);
        c.save();c.rotate(angle,x,y);paint.setColor(0xFF17181E);paint.setStyle(Paint.Style.FILL);c.drawCircle(x,y,r,paint);
        if(artwork!=null){c.save();Path clip=new Path();clip.addCircle(x,y,r*.95f,Path.Direction.CW);c.clipPath(clip);float scale=2*r*.95f/Math.min(artwork.getWidth(),artwork.getHeight());float w=artwork.getWidth()*scale,h=artwork.getHeight()*scale;paint.setAlpha(255);c.drawBitmap(artwork,null,new RectF(x-w/2,y-h/2,x+w/2,y+h/2),paint);c.restore();}
        else {paint.setShader(new SweepGradient(x,y,new int[]{0xFF252630,0xFF7B729E,0xFF22232B,0xFF615D77,0xFF252630},null));c.drawCircle(x,y,r*.95f,paint);paint.setShader(null);}
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1);paint.setColor(artwork==null?0x4440404A:0x22FFFFFF);for(float ring=.3f;ring<.98f;ring+=.027f)c.drawCircle(x,y,r*ring,paint);
        paint.setStyle(Paint.Style.FILL);paint.setColor(0xAA15141E);c.drawCircle(x,y,r*.27f,paint);paint.setColor(0xFFCAC2E3);c.drawCircle(x,y,r*.105f,paint);paint.setColor(0xFF15121D);c.drawCircle(x,y,r*.062f,paint);c.restore();
    }
}
