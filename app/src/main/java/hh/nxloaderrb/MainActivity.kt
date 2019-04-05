package hh.nxloaderrb

import android.app.AlertDialog
import android.app.Dialog
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.CompoundButton
import com.github.angads25.filepicker.model.DialogConfigs
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import kotlinx.android.synthetic.main.activity_main.*
import com.github.angads25.filepicker.model.DialogProperties
import java.io.File
import com.github.angads25.filepicker.view.FilePickerDialog
import com.github.angads25.filepicker.controller.DialogSelectionListener
import kotlin.collections.ArrayList
import android.os.Environment.MEDIA_MOUNTED
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import hh.nxloaderrb.utils.SDCardsUtils
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread


class MainActivity : AppCompatActivity() {
    private val APX_VID = 0x0955
    private val APX_PID = 0x7321
    private var ACTION_USB_PERMISSION ="hh.USB_PERMISSION"
    private  var dialog:FilePickerDialog?=null
    private var usbreceiver:BroadcastReceiver?=null
    private var useSX=false
    private var autointent:Intent?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var sharepreferences=getSharedPreferences("Config", Context.MODE_PRIVATE)
        val payload_name = sharepreferences.getString("binpath", null)
        if(payload_name!=null)
            filepath.text=payload_name
        setSlideMenu()
        setItems()
        RegisterReceiver()
    }

    override fun onResume() {
        super.onResume()
        UsbPermissionCheck()

    }

    fun RegisterReceiver()
    {
        if(usbreceiver==null) {
            usbreceiver = usbBroadcastreceiver()
            var filter = IntentFilter()
            filter.addAction(ACTION_USB_PERMISSION)
            filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            registerReceiver(usbreceiver, filter)
        }
        else{
            unregisterReceiver(usbreceiver)
        }
    }
    override fun onDestroy() {
        if (usbreceiver != null) {
            unregisterReceiver(usbreceiver)
            super.onDestroy()
        }
    }
    fun UsbPermissionCheck():Boolean
    {
        var connection=false
        var manager =  getSystemService(Context.USB_SERVICE) as UsbManager

        var deviceList = manager.getDeviceList();
        var  deviceIterator = deviceList.values.iterator();
        var mPermissionIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), 0)
        for(i in manager.deviceList)
        {
            var device=i.value
            if(manager.hasPermission(device))
            {
                connection=true
                switchstatus.text=getString(R.string.deviceconnection)
                switchstatus.setTextColor(resources.getColor(R.color.light_green))
            }
            else {
                connection=false
                switchstatus.text=getString(R.string.devicenotconnection)
                switchstatus.setTextColor(resources.getColor(R.color.red))
                manager.requestPermission(device, mPermissionIntent)
            }

        }
        return connection
    }

//    File permission check
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode)
        {
            FilePickerDialog.EXTERNAL_READ_PERMISSION_GRANT->{
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(dialog!=null)
                    {
                        //Show dialog if the read permission has been granted.
                        dialog!!.show();
                    }
                }
                else {
                    //Permission has not been granted. Notify the user.
                    dialog!!.dismiss()
                }
            }
        }
    }

//    USB broadcasterreceiver
    fun usbBroadcastreceiver():BroadcastReceiver{
        var usbreceiver=object:BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                var action=intent!!.action
                when(action)
                {
                    "hh.USB_PERMISSION"->{
                        synchronized(this) {
                            val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

                            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                                device?.apply {
                                    //call method to set up device communication

                                }
                            } else {

                            }
                        }
                    }
                    UsbManager.ACTION_USB_DEVICE_ATTACHED->
                        if(UsbPermissionCheck())injectauto()

                    UsbManager.ACTION_USB_DEVICE_DETACHED->
                    {
                        switchstatus.text=getString(R.string.devicenotconnection)
                        switchstatus.setTextColor(resources.getColor(R.color.red))
                    }
                }
            }
        }
        return usbreceiver
    }

//    set the slide menu
    fun setSlideMenu()
    {
        var menuarray=resources.getStringArray(R.array.menu)
        val item1 = SecondaryDrawerItem().withIdentifier(1).withName(menuarray.get(1)).withSelectable(false)
//        bartitle.text=menuarray.get(0)
        bartitle.text=getString(R.string.app_name)+" "+packageManager.getPackageInfo(packageName,0).versionName
        var leftmenu= DrawerBuilder()
                .withActivity(this@MainActivity)
                .addDrawerItems(
                        item1
                )
                .withOnDrawerItemClickListener(object : Drawer.OnDrawerItemClickListener{
                    override fun onItemClick(view: View?, position: Int, drawerItem: IDrawerItem<*, *>?): Boolean {
                        when(drawerItem!!.identifier.toInt())
                        {
                            1->{
                                startActivity(Intent(this@MainActivity,AboutActivity::class.java))
                            }

                        }

                        return false
                    }

                })
                .build()
        slidemenu.setOnClickListener {
            leftmenu.openDrawer()
        }
    }

    fun setFile()
    {

        filebtn.setOnClickListener {
            if(SDCardsUtils.getSDCard1(this)!=null){
                var sddialog=AlertDialog.Builder(this)
                sddialog.setTitle(R.string.sdchoose)
                val items = arrayOf(getString(R.string.insd),getString(R.string.outsd))

                sddialog.setItems(items,object:DialogInterface.OnClickListener{
                    override fun onClick(dialog1: DialogInterface?, which: Int) {
                        when(which)
                        {
                            0-> {
                                var folder = SDCardsUtils.getSDCard0(this@MainActivity)
                                filedialog(folder)
                                dialog!!.show()
                            }

                            1->            {
                                var folder=SDCardsUtils.getSDCard1(this@MainActivity)
                                filedialog(folder)
                                dialog!!.show()

                            }
                        }
                    }

                }).show()
            }
            else {
                var folder = SDCardsUtils.getSDCard0(this@MainActivity)
                filedialog(folder)
                dialog!!.show()
            }
        }
    }

    fun filedialog(folder:String){
        var properties = DialogProperties()
        properties.selection_mode = DialogConfigs.SINGLE_MODE
        properties.selection_type = DialogConfigs.FILE_SELECT
        properties.root = File(folder)
        properties.error_dir = File(DialogConfigs.DEFAULT_DIR)
        properties.offset = File(DialogConfigs.DEFAULT_DIR)
        var exten=ArrayList<String>()
        exten.add("bin")
        properties.extensions=exten.toTypedArray()
        dialog = FilePickerDialog(this@MainActivity, properties)
        dialog!!.setTitle(getString(R.string.fileselmsg))
        dialog!!.setPositiveBtnName(getString(R.string.filesepo))
        dialog!!.setNegativeBtnName(getString(R.string.filesena))

        dialog!!.setDialogSelectionListener (object:DialogSelectionListener{
            override fun onSelectedFilePaths(files: Array<out String>?) {
                filepath.text=files!!.get(0)
                var sharepreferences=getSharedPreferences("Config", Context.MODE_PRIVATE)
                var shareditor=sharepreferences.edit().putString("binpath",files!!.get(0))
                shareditor.apply()
            }
        })
    }
//    Init the elements and functions
    fun setItems()
    {
        setFile()
        var sharepreferences=getSharedPreferences("Config", Context.MODE_PRIVATE)
        useSX = sharepreferences.getBoolean("useSX", false)
        setsxosswitch.isChecked=useSX
        if(useSX){
            filebtn.isClickable=false
            filepath.text=getString(R.string.sxossetdes)
            fileselection.setBackgroundColor(resources.getColor(R.color.gray))
        }
        setsxosswitch.setOnCheckedChangeListener(object:CompoundButton.OnCheckedChangeListener{
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                var sharepreferences=getSharedPreferences("Config", Context.MODE_PRIVATE)
                var shareditor:SharedPreferences.Editor?=null
                if(isChecked)
                {
                    shareditor=sharepreferences.edit().putBoolean("useSX",true)
                    filebtn.isClickable=false
                    filepath.text=getString(R.string.sxossetdes)
                    fileselection.setBackgroundColor(resources.getColor(R.color.gray))
                }
                else{
                    shareditor=sharepreferences.edit().putBoolean("useSX",false)
                    filebtn.isClickable=true
                    var payload_name = sharepreferences.getString("binpath", null)
                    filepath.text=payload_name
                    fileselection.setBackgroundColor(resources.getColor(R.color.white))

                }
                shareditor!!.apply()
            }
        })
        injectauto()
        injection.setOnClickListener {
            if(UsbPermissionCheck()) {
                runOnUiThread {

                    injectionloading.visibility = View.VISIBLE
                    injectionloading.bringToFront()
                    injectBin()
                }
            }
            else
                    Toastmessage(getString(R.string.devicenotconnection))

        }


        autoinjection.setOnCheckedChangeListener(object:CompoundButton.OnCheckedChangeListener{
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                if(isChecked)
                {
                   sharepreferences.edit().putBoolean("autoinject",true).apply()

//                    autointent=Intent(this@MainActivity,AutoInjectService::class.java)
//                    startService(autointent)
                }
                else {
                   sharepreferences.edit().putBoolean("autoinject",false).apply()

//                    if(autointent!=null)
//                        stopService(autointent)
                }
            }

        })
    }

    fun injectauto()
    {
        var sharepreferences=getSharedPreferences("Config", Context.MODE_PRIVATE)

        var autoinject=sharepreferences.getBoolean("autoinject",false)
        if(autoinject)
        {
            autoinjection.isChecked=true
            var binpath = sharepreferences.getString("binpath", null)
            if(binpath!=null)
                injectBin()
            else
                Toastmessage(getString(R.string.filepathsrc))
        }
        else autoinjection.isChecked=false
    }
    fun injectBin()
    {
        var usbManager=getSystemService(Context.USB_SERVICE) as UsbManager
        var devicelist=usbManager.deviceList

        for(a in devicelist)
        {
            if(a.value.productId==APX_PID&&a.value.vendorId==APX_VID) {
//                    Thread(Runnable {
                var u=PrimaryLoader()
                if(u!=null) {
                    var sharepreferences=getSharedPreferences("Config", Context.MODE_PRIVATE)
                    var useSX = sharepreferences.getBoolean("useSX", false)
                    u.handleDevice(this, a.value, object:PrimaryLoader.Injectionprogress {
                        override fun onFailed(errormsg: String) {
                            doAsync {
                                injectionloading.visibility = View.GONE
                                Toastmessage(errormsg)
                            }
                        }


                        override fun onCompleted() {
                            doAsync {
                                injectionloading.visibility = View.GONE
                                Toastmessage(getString(R.string.injectsuccess))
                            }
                        }
                    },useSX
                    )
                }
//                    }).start()

            }
        }
    }
    fun Toastmessage(msg:String)
    {
        object : Thread() {
            override fun run() {
                Looper.prepare()
                //                CumToast.show(context,messagestr);
                if (msg != "")
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show()
                Looper.loop()
            }

        }.start()
    }
}
