package hh.nxloaderrb

import android.content.*
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.github.angads25.filepicker.model.DialogConfigs
import com.mikepenz.materialdrawer.AccountHeader
import com.mikepenz.materialdrawer.AccountHeaderBuilder
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IProfile
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import com.github.angads25.filepicker.model.DialogProperties
import java.io.File
import com.github.angads25.filepicker.view.FilePickerDialog
import com.github.angads25.filepicker.controller.DialogSelectionListener
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {
    private val APX_VID = 0x0955
    private val APX_PID = 0x7321
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var sharepreferences=getSharedPreferences("Config", Context.MODE_PRIVATE)
        val payload_name = sharepreferences.getString("binpath", null)
        if(payload_name!=null)
            filepath.text=payload_name
        setSlideMenu()
        setItems()
    }

    fun usbBroadcastreceiver(){
       var ACTION_USB_PERMISSION ="hh.USB_PERMISSION"
        var usbreceiver=object:BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                var action=intent!!.action
                if(ACTION_USB_PERMISSION.equals(action))
                {
                    synchronized(this) {
                        val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            device?.apply {
                                //call method to set up device communication
                            }
                        } else {
                            Log.d(TAG, "permission denied for device $device")
                        }
                    } 
                }
            }

        }
    }
    fun setSlideMenu()
    {
        var menuarray=resources.getStringArray(R.array.menu)
        val item1 = SecondaryDrawerItem().withIdentifier(1).withName(menuarray.get(0)).withSelectable(false)
        val item2 = SecondaryDrawerItem().withIdentifier(2).withName(menuarray.get(1)).withSelectable(false)
        val item3 = SecondaryDrawerItem().withIdentifier(3).withName(menuarray.get(2)).withSelectable(false)


        var leftmenu= DrawerBuilder()
                .withActivity(this@MainActivity)
                .addDrawerItems(
                        item1,
                        item2,
                        item3
                )
                .withOnDrawerItemClickListener(object : Drawer.OnDrawerItemClickListener{
                    override fun onItemClick(view: View?, position: Int, drawerItem: IDrawerItem<*, *>?): Boolean {
                        when(drawerItem!!.identifier.toInt())
                        {
                            1->{

                            }
                            2->{

                            }
                            3->{

                            }

                        }

                        return false
                    }

                })
                .build()
    }

    fun setItems()
    {
        val properties = DialogProperties()
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = File(DialogConfigs.DEFAULT_DIR);
        properties.error_dir = File(DialogConfigs.DEFAULT_DIR);
        properties.offset = File(DialogConfigs.DEFAULT_DIR);
        var binextension=ArrayList<String>()
        binextension.add("bin")
        properties.extensions =binextension.toTypedArray() ;
        val dialog = FilePickerDialog(this@MainActivity, properties)
        dialog.setTitle(getString(R.string.fileselmsg))
        dialog.setDialogSelectionListener (object:DialogSelectionListener{
            override fun onSelectedFilePaths(files: Array<out String>?) {
                filepath.text=files!!.get(0)
                var sharepreferences=getSharedPreferences("Config", Context.MODE_PRIVATE)
                var shareditor=sharepreferences.edit().putString("binpath",files!!.get(0))
                shareditor.apply()
            }

        })
        filebtn.setOnClickListener {
         dialog.show()
        }

        injection.setOnClickListener {
            val intent = intent
            var usbManager=getSystemService(Context.USB_SERVICE) as UsbManager
            var devicelist=usbManager.deviceList

            for(a in devicelist)
            {
                if(a.value.productId==APX_PID&&a.value.vendorId==APX_VID) {
                    var u=USBprocess()
                    u.init()
                    u.handleDevice(this,a.value)
                }

            }
        }
    }


}
