package no.barentswatch.implementation;

import android.app.Application;

public class ECatchApp extends Application {

	 private static ECatchApp mInstance;


	 public static ECatchApp getInstance() {
	  return mInstance;
	 }

	 @Override
	 public void onCreate() {
	  super.onCreate();
	  mInstance = this;
	 }
	}