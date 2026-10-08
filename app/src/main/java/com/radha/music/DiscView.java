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
        paint.setShader(new RadialGradient(x,y,r*1.12f,new int[]{0x22444444,0x00444444},null,Shader.TileMode.CLAMP));c.drawCircle(x,y,r*1.12f,paint);paint.setShader(null);
        c.save();c.rotate(angle,x,y);paint.setColor(0xFF17181E);paint.setStyle(Paint.Style.FILL);c.drawCircle(x,y,r,paint);
        paint.setShader(new SweepGradient(x,y,new int[]{0xFF101112,0xFF464749,0xFF151617,0xFF303133,0xFF101112},null));c.drawCircle(x,y,r*.98f,paint);paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1);paint.setColor(0x554B4C4E);for(float ring=.49f;ring<.98f;ring+=.022f)c.drawCircle(x,y,r*ring,paint);paint.setStyle(Paint.Style.FILL);
        if(artwork!=null){c.save();Path clip=new Path();clip.addCircle(x,y,r*.46f,Path.Direction.CW);c.clipPath(clip);float scale=2*r*.46f/Math.min(artwork.getWidth(),artwork.getHeight());float w=artwork.getWidth()*scale,h=artwork.getHeight()*scale;c.drawBitmap(artwork,null,new RectF(x-w/2,y-h/2,x+w/2,y+h/2),paint);c.restore();}
        else {paint.setShader(new LinearGradient(x,y-r*.46f,x,y+r*.46f,0xFFFFAD7F,0xFFBF665B,Shader.TileMode.CLAMP));c.drawCircle(x,y,r*.46f,paint);paint.setShader(null);}
        paint.setColor(0xFF292A2C);c.drawCircle(x,y,r*.09f,paint);paint.setColor(0xFFB8B8B8);c.drawCircle(x,y,r*.063f,paint);paint.setColor(0xFF121315);c.drawCircle(x,y,r*.036f,paint);c.restore();
    }
}
