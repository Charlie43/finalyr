package com.project.charlie.cyrogenic;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.google.example.games.basegameutils.GameHelper;
import com.google.example.games.basegameutils.GameHelper.GameHelperListener;
import com.project.charlie.cyrogenic.android.ActionResolver;
import com.project.charlie.cyrogenic.game.Cyrogenic;

public class AndroidLauncher extends AndroidApplication implements ActionResolver, GameHelperListener {
    private static final int FITNESS_REQUEST = 1;
    ActionResolverAndroid actionResolverAndroid;
    GameHelper gameHelper;

    public AndroidLauncher() {
        actionResolverAndroid = new ActionResolverAndroid(this);
//        gameHelper = new GameHelper(this, GameHelper.CLIENT_GAMES);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();

        initialize(new Cyrogenic(this), config);

//        gameHelper.setup(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
//        requestPermissions();
    }


    @Override
    protected void onStart() {
        super.onStart();
        gameHelper.onStart(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        gameHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStop() {
        super.onStop();
        gameHelper.onStop();
    }

    public void requestPermissions() {
        Gdx.app.log("AL", "Requesting permissions..");
        ActivityCompat.requestPermissions(this,
                new String[]{"FITNESS_ACTIVITY_READ"}, FITNESS_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Gdx.app.log("AL", "Received permission result");
        switch (requestCode) {
            case FITNESS_REQUEST:
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Gdx.app.log("Permissions", "Permissions granted");

                }
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void connectToFitnessApi() {
        actionResolverAndroid.connectToFitnessApi();
    }

    @Override
    public void signIn() {
        try {
            Gdx.app.log("AL", "Sign in started..");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    gameHelper.beginUserInitiatedSignIn();
                }
            });

        } catch (Exception e) {
            Gdx.app.log("AL", "Exception on sign in - " + e.getMessage());
        }
    }

    @Override
    public void signOut() {
        try {
            Gdx.app.log("AL", "Sign out started..");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    gameHelper.signOut();
                }
            });
        }catch(Exception e) {
            Gdx.app.log("AL", "Exception on sign out - " + e.getMessage());
        }
    }

    @Override
    public boolean isSignedIn() {
        return gameHelper.isSignedIn();

    }

    @Override
    public void onSignInFailed() {
        Gdx.app.log("AL", "Sign in failed");

    }

    @Override
    public void onSignInSucceeded() {
        Gdx.app.log("AL", "Sign in succeeded");


    }
}
