package hh.nxloaderrb

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_about.*
import android.support.v4.view.accessibility.AccessibilityEventCompat.setAction
import android.content.Intent
import android.net.Uri


class AboutActivity : AppCompatActivity() {
    private var url1="https://github.com/DavidBuchanan314/NXLoader"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        var menuarray=resources.getStringArray(R.array.menu)
        bartitle.text=menuarray.get(1)
        slidemenu.setOnClickListener {
            onBackPressed()
        }
        orauthor.setText(getString(R.string.orauthor,url1))
        orauthor.setOnClickListener{
            openurl(url1)
        }
    }
    fun openurl(url:String)
    {
        var uri = Uri.parse(url)
        val intent = Intent()
        intent.action = "android.intent.action.VIEW"
        intent.data = uri
        startActivity(intent)
    }
}
