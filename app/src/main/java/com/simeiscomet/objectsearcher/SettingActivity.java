package com.simeiscomet.objectsearcher;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class SettingActivity extends Activity {

    private static Toast _toast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences( this );
        final SharedPreferences.Editor prefEditor = pref.edit();

        CheckBox checkMultiThread = (CheckBox)findViewById( R.id.checkMultiThread );
        checkMultiThread.setChecked(pref.getBoolean("isUseMultiThread", true));
        checkMultiThread.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                prefEditor.putBoolean("isUseMultiThread", isChecked );
                prefEditor.commit();

                SeekBar seekBarCoreNum = (SeekBar)findViewById( R.id.seekBarThreadNum );

                if( isChecked ){
                    seekBarCoreNum.setEnabled( true );
                } else {
                    seekBarCoreNum.setEnabled( false );
                }
            }
        });

        SeekBar seekBarThreadNum = (SeekBar)findViewById( R.id.seekBarThreadNum );
        seekBarThreadNum.setProgress( pref.getInt("useThreadNum", 5 ) );

        final TextView textThreadNum = (TextView)findViewById( R.id.textThreadNum );
        textThreadNum.setText( "使用スレッド数：" + seekBarThreadNum.getProgress() );

        seekBarThreadNum.setOnSeekBarChangeListener( new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged( SeekBar seekBar, int progress, boolean fromUser )
            {
                int num = seekBar.getProgress()+1;
                prefEditor.putInt("useThreadNum", num );
                prefEditor.commit();
                textThreadNum.setText( "使用スレッド数：" + num );
                //_setToast( textThreadNum.getText().toString() );
            }

            @Override
            public void onStartTrackingTouch( SeekBar seekBar )
            {
                int num = seekBar.getProgress()+1;
                prefEditor.putInt("useThreadNum", num );
                prefEditor.commit();
                textThreadNum.setText("使用スレッド数：" + num);
                //_setToast( textThreadNum.getText().toString() );
            }

            @Override
            public void onStopTrackingTouch( SeekBar seekBar )
            {
                int num = seekBar.getProgress()+1;
                prefEditor.putInt("useThreadNum", num );
                prefEditor.commit();
                textThreadNum.setText("使用スレッド数：" + num );
                //_setToast( textThreadNum.getText().toString() );
            }
        });
    }

    private void _setToast( String message )
    {
        if( _toast != null ) _toast.cancel();
        _toast = Toast.makeText( getApplicationContext(), message, Toast.LENGTH_LONG );
        _toast.show();
    }
}
