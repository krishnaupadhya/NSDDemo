package com.example.krishnaupadhya.nsddemo.ui.home

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.example.krishnaupadhya.nsddemo.R
import com.example.krishnaupadhya.nsddemo.ui.nsd.NsdMasterActivity
import com.example.krishnaupadhya.nsddemo.ui.nsd.NsdSlaveActivity
import kotlinx.android.synthetic.main.activity_home.*

class HomeActivity : AppCompatActivity(), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        btn_master.setOnClickListener(this)
        btn_slave.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.btn_master -> {
                val intent = Intent(this, NsdMasterActivity::class.java)
                startActivity(intent)
            }
            R.id.btn_slave -> {
                val intent = Intent(this, NsdSlaveActivity::class.java)
                startActivity(intent)
            }
        }
    }
}
