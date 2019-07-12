package com.akeno0810.kancolleHtml5Browser;

import java.util.Random;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.os.SystemClock;

public class MainActivity extends Activity {

    final String KANCOLLE_URL = "http://www.dmm.com/netgame/social/-/gadgets/=/app_id=854854/";
    final String[] SERVER_IP = {
            "203.104.209.71",
            "203.104.209.87",
            "125.6.184.215",
            "203.104.209.183",
            "203.104.209.150",
            "203.104.209.134",
            "203.104.209.167",
            "203.104.248.135",
            "125.6.189.7",
            "125.6.189.39",
            "125.6.189.71",
            "125.6.189.103",
            "125.6.189.135",
            "125.6.189.167",
            "125.6.189.215",
            "125.6.189.247",
            "203.104.209.23",
            "203.104.209.39",
            "203.104.209.55",
            "203.104.209.102",
            //"203.104.209.7"
    };
    final String[] USER_AGENT_PC_LIST = {
            "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.84 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.87 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36 Edge/16.16299",
            "Mozilla/5.0 (Windows NT x.y; Win64; x64; rv:10.0) Gecko/20100101 Firefox/60.0.2",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.84 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/604.4.7 (KHTML, like Gecko) Version/11.0.2 Safari/604.4.7"
    };
    final String[] USER_AGENT_SP_ADD_LIST = {
            "Gecko) Chrome/68.0.3440.85 Mobile Safari/537.36"
    };

    WebView myWebView;
    String USER_AGENT_PC;
    String USER_AGENT_SP;
    // オーディオモード保持
    AudioManager mAudioManager;
    int savedAudioMode;
    // 輝度保持
    float brightness;
    // 輝度を落とす待ち時間
    int waitSecond = 0;
    final int waitLimit = 30;
    ScaleGestureDetector scaleGesture;
    float scaleFactor = 1.0f;
    boolean isScaling = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        stickyImmersiveMode();

        Random r = new Random();
        myWebView = findViewById(R.id.webView);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // 画面輝度タイマー設定
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        brightness = lp.screenBrightness;
        final Handler handler = new Handler();
        final Runnable rr = new Runnable() {
            @Override
            public void run() {
                // 60秒放置で輝度を下げる
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                if (waitSecond > waitLimit) {
                    lp.screenBrightness = 0.0F;
                    //getWindow().setAttributes(lp);
                } else {
                    waitSecond++;
                }
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(rr);

        // 拡縮移動設定
        scaleGesture = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                isScaling = true;
                return super.onScaleBegin(detector);
            }
            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                scaleFactor = detector.getScaleFactor();
                if (scaleFactor < 1.0f) {
                    scaleFactor = 1.0f;
                }
                // スケール終了
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        isScaling = false;
                    }
                }, 500);
                super.onScaleEnd(detector);
            }
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float sf = detector.getScaleFactor() * scaleFactor;
                if (sf < 1.0f) {
                    sf = 1.0f;
                }
                myWebView.setScaleX(sf);
                myWebView.setScaleY(sf);
                return super.onScale(detector);
            }
        });

        // ユーザーエージェント生成
        USER_AGENT_PC = USER_AGENT_PC_LIST[r.nextInt(USER_AGENT_PC_LIST.length)];
        USER_AGENT_SP = myWebView.getSettings().getUserAgentString();
        USER_AGENT_SP = USER_AGENT_SP.split("Gecko", 0)[0] + USER_AGENT_SP_ADD_LIST[r.nextInt(USER_AGENT_SP_ADD_LIST.length)];

        // JSとキャッシュの有効化
        myWebView.setWebViewClient(new ViewClient());
        myWebView.getSettings().setAppCacheEnabled(true);
        myWebView.getSettings().setJavaScriptEnabled(true);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptThirdPartyCookies(myWebView, true);

        // ミュート設定
        final boolean isMute = getIntent().getBooleanExtra("mute", false);
        if (isMute) {
            for (String ip : SERVER_IP) {
                cookieManager.setCookie(ip, "vol_bgm=0; domain=" + ip + "; path=/kcs2");
                cookieManager.setCookie(ip, "vol_se=0; domain=" + ip + "; path=/kcs2");
                cookieManager.setCookie(ip, "vol_voice=0; domain=" + ip + "; path=/kcs2");
            }
        }
        cookieManager.flush();

        // デバッグ用キャッシュ全消し
        //cookieManager.removeAllCookies(null);

        myWebView.loadUrl(KANCOLLE_URL);

    }

    @Override
    public void onBackPressed() {
        // ゲーム画面では戻るキーを無効化
        if (!myWebView.getUrl().equals(KANCOLLE_URL)) {
            if (myWebView.canGoBack()) {
                myWebView.goBack();
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    protected void onPause() {
        myWebView.onPause();
        myWebView.pauseTimers();
        super.onPause();
        //setAudioFocus(false);
    }

    @Override
    protected void onDestroy() {
        if (myWebView != null) {
            myWebView.destroy();
            myWebView = null;
        }
        //setAudioFocus(false);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        myWebView.resumeTimers();
        myWebView.onResume();
        stickyImmersiveMode();

        //setAudioFocus(true);
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        Log.d("touchEvent", motionEvent.getToolType(0) + ":" + motionEvent.getActionMasked());

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = brightness;
        getWindow().setAttributes(lp);
        // 放置カウントを0にする
        waitSecond = 0;

        // スケール中は他を動作させない
        /*
        scaleGesture.onTouchEvent(motionEvent);
        if(isScaling){
            return true;
        }
        */

        if (motionEvent.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER) {
            // 指での操作の場合
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    // タッチ（押下）時
                    // 現在のデータからプロパティ作成
                    MotionEvent.PointerProperties[] pp = {new MotionEvent.PointerProperties()};
                    motionEvent.getPointerProperties(0, pp[0]);
                    MotionEvent.PointerCoords[] pc = {new MotionEvent.PointerCoords()};
                    motionEvent.getPointerCoords(0, pc[0]);
                    pp[0].toolType = MotionEvent.TOOL_TYPE_MOUSE;

                    // マウスでのタッチイベントを発生させる
                    long time = SystemClock.uptimeMillis();
                    MotionEvent event = MotionEvent.obtain(time, time, MotionEvent.ACTION_MOVE, 1, pp, pc, 0, 0, 0, 0, 1, 0, InputDevice.SOURCE_MOUSE, 0);
                    myWebView.onTouchEvent(event);
            }
        }
        // 通常処理
        return super.dispatchTouchEvent(motionEvent);
    }


    // 全画面にしてAndroidのコントロールを隠す
    private void stickyImmersiveMode() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        );
    }

    private void setAudioFocus(boolean setFocus) {
        AudioManager audioManager = mAudioManager;
        if (audioManager != null) {
            if (setFocus) {
                savedAudioMode = audioManager.getMode();
                // Request audio focus before making any device switch.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build();
                    AudioFocusRequest focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                            .setAudioAttributes(playbackAttributes)
                            .setAcceptsDelayedFocusGain(true)
                            .setOnAudioFocusChangeListener(new AudioManager.OnAudioFocusChangeListener() {
                                @Override
                                public void onAudioFocusChange(int i) {
                                }
                            })
                            .build();
                    audioManager.requestAudioFocus(focusRequest);
                } else {
                    audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL,
                            AudioManager.AUDIOFOCUS_GAIN);
                }
                /*
                 * Start by setting MODE_IN_COMMUNICATION as default audio mode. It is
                 * required to be in this mode when playout and/or recording starts for
                 * best possible VoIP performance. Some devices have difficulties with speaker mode
                 * if this is not set.
                 */
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            } else {
                audioManager.setMode(savedAudioMode);
                audioManager.abandonAudioFocus(null);
            }
        }
    }

    public final class ViewClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            // 読み込み開始時
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            //ロード開始時にやりたい事を書く
            super.onLoadResource(view, url);
            if (view.getUrl().equals(KANCOLLE_URL)) {
                // ユーザーエージェントをPCにして横画面に固定
                view.getSettings().setUserAgentString(USER_AGENT_PC);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                stickyImmersiveMode();
                // ゲーム画面をブラウザに合わせる
                String script = "javascript:(($,_)=>{const html=$.documentElement,gf=$.getElementById('game_frame'),gs=gf.style,gw=gf.offsetWidth,gh=gw*.6;let vp=$.querySelector('meta[name=viewport]'),t=0;vp||(vp=$.createElement('meta'),vp.name='viewport',$.querySelector('head').appendChild(vp));vp.content='width='+gw;'orientation'in _&&html.webkitRequestFullscreen&&html.webkitRequestFullscreen();html.style.overflow='hidden';$.body.style.cssText='min-width:0;overflow:hidden;margin:0';$.querySelector('.dmm-ntgnavi').style.display='none';$.querySelector('.area-naviapp').style.display='none';$.getElementById('ntg-recommend').style.display='none';gs.position='fixed';gs.marginRight='auto';gs.marginLeft='auto';gs.top='-16px';gs.right='0';gs.zIndex='100';gs.transformOrigin='50%25%2016px';if(!_.kancolleFit){const k=()=>{const w=html.clientWidth,h=_.innerHeight;w/h<1/.6?gs.transform='scale('+w/gw+')':gs.transform='scale('+h/gh+')';w<gw?gs.left='-'+(gw-w)/2+'px':gs.left='0'};_.addEventListener('resize',()=>{clearTimeout(t);t=setTimeout(k,10)});_.kancolleFit=k}kancolleFit()})(document,window)";
                view.loadUrl(script);
            }

        }

        @Override
        public void onPageFinished(WebView view, String url) {
            //ロード完了時にやりたい事を書く
            super.onPageFinished(view, url);
            // 画面を常にON
            //myWebView.setKeepScreenOn(true);
            if (view.getUrl().equals(KANCOLLE_URL)) {
                String script = "javascript:(($,_)=>{const html=$.documentElement,gf=$.getElementById('game_frame'),gs=gf.style,gw=gf.offsetWidth,gh=gw*.6;let vp=$.querySelector('meta[name=viewport]'),t=0;vp||(vp=$.createElement('meta'),vp.name='viewport',$.querySelector('head').appendChild(vp));vp.content='width='+gw;'orientation'in _&&html.webkitRequestFullscreen&&html.webkitRequestFullscreen();html.style.overflow='hidden';$.body.style.cssText='min-width:0;overflow:hidden;margin:0';$.querySelector('.dmm-ntgnavi').style.display='none';$.querySelector('.area-naviapp').style.display='none';$.getElementById('ntg-recommend').style.display='none';gs.position='fixed';gs.marginRight='auto';gs.marginLeft='auto';gs.top='-16px';gs.right='0';gs.zIndex='100';gs.transformOrigin='50%25%2016px';if(!_.kancolleFit){const k=()=>{const w=html.clientWidth,h=_.innerHeight;w/h<1/.6?gs.transform='scale('+w/gw+')':gs.transform='scale('+h/gh+')';w<gw?gs.left='-'+(gw-w)/2+'px':gs.left='0'};_.addEventListener('resize',()=>{clearTimeout(t);t=setTimeout(k,10)});_.kancolleFit=k}kancolleFit()})(document,window)";
                view.loadUrl(script);
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            if (!request.getUrl().toString().equals(KANCOLLE_URL)) {
                // ゲーム画面のURLでなかった場合スマートフォンのユーザーエージェントに変更し画面固定解除
                view.getSettings().setUserAgentString(USER_AGENT_SP);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                view.loadUrl(request.getUrl().toString());
            }
            return false;
        }
    }
}
