package me.contactapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlin.concurrent.thread

data class Contact(val name: String?, val phoneNumber: String?, val email: String?)

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var permissionContainer: View
    private lateinit var btnRequestPermission: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        permissionContainer = findViewById(R.id.permissionContainer)
        btnRequestPermission = findViewById(R.id.btnRequestPermission)

        btnRequestPermission.setOnClickListener {
            requestContactsPermission()
        }

        if (checkContactsPermission()) {
            initContactsList()
        } else {
            showPermissionRequest()
        }
    }

    private fun checkContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestContactsPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.READ_CONTACTS), 100
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initContactsList()
        }
    }

    private fun showPermissionRequest() {
        permissionContainer.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun initContactsList() {
        permissionContainer.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        recyclerView.layoutManager = LinearLayoutManager(this)

        thread {
            val contacts = fetchAllContacts()
            runOnUiThread {
                recyclerView.adapter = ContactAdapter(contacts) { contact ->
                    showContactDetails(contact)
                }
            }
        }
    }

    private fun showContactDetails(contact: Contact) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_contact_details, null)
        
        val nameTv = view.findViewById<TextView>(R.id.tvDetailName)
        val phoneTv = view.findViewById<TextView>(R.id.tvDetailPhone)

        nameTv.text = getString(R.string.name_label, contact.name ?: getString(R.string.unknown_name))
        phoneTv.text = getString(R.string.phone_label, contact.phoneNumber ?: getString(R.string.unknown_phone))

        dialog.setContentView(view)
        dialog.show()
    }
}

@SuppressLint("Range")
fun Context.fetchAllContacts(): List<Contact> {
    val list = mutableListOf<Contact>()
    contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null)?.use { cursor ->
        while (cursor.moveToNext()) {
            val name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
            val phone = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
            list.add(Contact(name, phone, null))
        }
    }
    return list.sortedBy { it.name?.lowercase() }
}

class ContactAdapter(
    private val contacts: List<Contact>,
    private val onClick: (Contact) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTv: TextView = view.findViewById(R.id.tvContactName)
        val avatarTv: TextView = view.findViewById(R.id.tvAvatar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = contacts[position]
        val name = contact.name ?: "Unknown"
        holder.nameTv.text = name
        holder.avatarTv.text = name.trim().take(1).uppercase()
        holder.itemView.setOnClickListener { onClick(contact) }
    }

    override fun getItemCount() = contacts.size
}
