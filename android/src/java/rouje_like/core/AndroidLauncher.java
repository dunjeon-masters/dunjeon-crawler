package rouje_like.core;

import android.util.Log;

import clojure.lang.RT;
import clojure.lang.Symbol;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.Game;

public class AndroidLauncher extends AndroidApplication {
	public void onCreate (android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
          RT.var("clojure.core", "require").invoke(Symbol.intern("rouje-like.core"));
		try {
			Game game = (Game) RT.var("rouje-like.core", "rouje-like").deref();
			Log.i("rouje-like", "b4 init");
			initialize(game);
			Log.i("rouje-like", "aft init");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
