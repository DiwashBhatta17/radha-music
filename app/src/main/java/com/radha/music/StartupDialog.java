package com.radha.music;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;

/** Launch-only presentation; a slow/offline provider must not block local music. */
final class StartupDialog extends Dialog {
    private long openedAt;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final java.util.function.BooleanSupplier homeReady;private final SessionCatalog catalog;
    StartupDialog(Activity activity,java.util.function.BooleanSupplier homeReady){this(activity,homeReady,SessionCatalog.INSTANCE);}
    StartupDialog(Activity activity,java.util.function.BooleanSupplier homeReady,SessionCatalog catalog){super(activity,android.R.style.Theme_Material_NoActionBar);this.catalog=catalog;setCancelable(false);this.homeReady=homeReady;
        FrameLayout root=new FrameLayout(activity);root.setBackgroundColor(0xFF101113);
        LinearLayout center=new LinearLayout(activity);center.setOrientation(LinearLayout.VERTICAL);center.setGravity(Gravity.CENTER);
        ImageView logo=new ImageView(activity);logo.setImageResource(R.drawable.radha_mark);center.addView(logo,new LinearLayout.LayoutParams(dp(76),dp(88)));
        TextView name=new TextView(activity);name.setText("Radha Music");name.setTextSize(28);name.setTextColor(0xFFF8F6F2);name.setGravity(Gravity.CENTER);name.setPadding(0,dp(20),0,dp(38));center.addView(name);
        ProgressBar progress=new ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal);progress.setIndeterminate(true);progress.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(0xFFFF795F));center.addView(progress,new LinearLayout.LayoutParams(dp(150),dp(4)));
        root.addView(center,new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER));TextView credit=new TextView(activity);credit.setText("Created, designed and developed by Diwash Bhatta");credit.setTextColor(0xFFA4A4A6);credit.setTextSize(10);credit.setGravity(Gravity.CENTER);credit.setPadding(dp(16),0,dp(16),dp(24));root.addView(credit,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));setContentView(root);
    }
    private int dp(int n){return Math.round(n*getContext().getResources().getDisplayMetrics().density);}
    @Override protected void onStart(){super.onStart();openedAt=SystemClock.elapsedRealtime();getWindow().setLayout(-1,-1);getWindow().getDecorView().setSystemUiVisibility(5894);main.post(check);}
    private final Runnable check=new Runnable(){public void run(){if((catalog.startupReady()&&homeReady.getAsBoolean())||SystemClock.elapsedRealtime()-openedAt>=90000)dismiss();else main.postDelayed(this,100);}};
    @Override public void dismiss(){main.removeCallbacksAndMessages(null);super.dismiss();}
}
