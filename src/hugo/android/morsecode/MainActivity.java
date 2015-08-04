package hugo.android.morsecode;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import model.MorseCodeModel;

public class MainActivity extends ActionBarActivity implements SensorEventListener {
	
	private final float LIM_LIGHT_VALUE = 100;
	private SensorManager mSensorManager;
	private Sensor mSensor;
	private boolean previous_state_pressed = false;
	private boolean pressed = false;
	private long current = 0;
	private long before = 0;
	boolean rebootTime = true;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
		mSensor = getSensorLight();
		
		if(checkFlashLightAvailability()){
			if (savedInstanceState == null) {
				getSupportFragmentManager().beginTransaction()
						.add(R.id.container, new PlaceholderFragment()).commit();
			}
		}else{
			Toast.makeText(this, "There is no flashlight available!", Toast.LENGTH_LONG).show();
		}
		
	}
	
	@Override
	protected void onResume() {
		
		mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);		
		
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		mSensorManager.unregisterListener(this);
		super.onPause();
	}

	private Sensor getSensorLight() {
		if(mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null){
			return mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
		}
		return null;
	}

	private boolean checkFlashLightAvailability() {
		return this.getPackageManager()
				.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		float level_light = event.values[0];
		
		if(rebootTime){
			rebootTime = false;
			before = System.currentTimeMillis();
		}
		
		if (pressed != previous_state_pressed){
			long intervalo = (current - before)/10;
			sendValues(level_light, intervalo, pressed);
			previous_state_pressed = pressed;
			rebootTime = true;
		}else{
			
			if(level_light < LIM_LIGHT_VALUE){
				pressed = true;
				current = System.currentTimeMillis();
				
			}else{
				pressed = false;
				current = System.currentTimeMillis();
			}
		}
		
		
		
		
	}

	private void sendValues(float value, double intervalo, boolean pressed) {
		((TextView) findViewById(R.id.textView1)).setText(String.valueOf(value));		
		((TextView) findViewById(R.id.textView2)).setText(String.valueOf(intervalo));
		((TextView) findViewById(R.id.textView3)).setText(String.valueOf(pressed));
				
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		
	}


	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment implements OnClickListener, Callback{
		
		private Button btSendMC;
		private EditText etMessage;
		private SurfaceView surfaceLight;
		private Camera mCamera;
		private SurfaceHolder mHolder;

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			
			btSendMC = (Button) rootView.findViewById(R.id.btSendMC);
			etMessage = (EditText) rootView.findViewById(R.id.eTMessage);
			surfaceLight = (SurfaceView) rootView.findViewById(R.id.surfaceViewLight);
			
			return rootView;

		}
		
		@Override
		public void onResume() {
			
			btSendMC.setOnClickListener(this);
			super.onResume();
		}


		@Override
		public void onClick(View v) {
			// TODO
			
			String message = etMessage.getEditableText().toString();
			if(!message.isEmpty()){
				boolean state = false;
				long[] pattern = MorseCodeModel.pattern(message);
				
				mHolder = surfaceLight.getHolder();
				mHolder.addCallback(this);
				
				for (int i = 1; i < pattern.length; i++) {
					try {
						turnLight(state = !state);
						Thread.sleep(pattern[i]);
					} catch (IOException e) {
						// TODO
						e.printStackTrace();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
			}
		}
			
		
		public void turnLight(boolean On) throws IOException{
			if (On){				
				mCamera = Camera.open();
				mCamera.setPreviewDisplay(mHolder);
				Parameters params = mCamera.getParameters();
				params.setFlashMode(Parameters.FLASH_MODE_TORCH);
				mCamera.setParameters(params);      
				mCamera.startPreview();
			}else{
				mCamera.stopPreview();
				mCamera.release();
			}

		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			mHolder = holder;
			try {
				mCamera.setPreviewDisplay(mHolder);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			mCamera.stopPreview();
			mHolder = null;
			
		}
		
		
		
		
	}	
}
