package it.uniupo.ebay_clone

import android.app.*
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.get
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import it.uniupo.ebay_clone.databinding.ActivityHomePageNewBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.*


//todo : res fragment
@Suppress("UNCHECKED_CAST")
class HomePage : AppCompatActivity(), MapsFragment.MapsResult, AdvancedResearchFragment.PosResult {
    //var di binding
    private lateinit var binding: ActivityHomePageNewBinding

    private var homeFragment = HomeFragment()
    private var advancedResearchFragment = AdvancedResearchFragment()
    private var resultFragment = ResultFragment()
    private var prefFragment = PrefFragment()

    private var auth: FirebaseAuth? = null
    private lateinit var db: FirebaseFirestore
    private lateinit var currentUser: User
    private var mapsFragment = MapsFragment()

    //toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    private var resFragment = Misc_res_fragment()

    private var fragmentList = ArrayList<Fragment>(4)
    private var current: Int = 0

    private lateinit var indirizzo: String
    private var lat: Double = 0.0
    private var long: Double = 0.0
    var pos: ArrayList<Double> = ArrayList(2)
    private var checkPos = false
    private var code = 0


    override fun getSearchResults(result: ArrayList<String>) {
        var bundle = Bundle()
        bundle.putString("code", "1")
        bundle.putStringArrayList("searchResult", result)
        advancedResearchFragment.arguments = bundle
        goneDividers()
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,  // Animation for the entering fragment
                R.anim.slide_out_left, // Animation for the exiting fragment
                R.anim.slide_in_left,  // Animation for the entering fragment (reverse)
                R.anim.slide_out_right   // Animation for the exiting fragment (reverse)
            )
            .detach(advancedResearchFragment)
            .attach(resultFragment)
            .commit()
        current = 3
        binding.dividerResult.visibility = View.VISIBLE
    }

    override fun getMapsResults(titolo: String, latitude: Double, longitude: Double) {
        MainScope().launch {
            withContext(Dispatchers.Main) {
                lat = latitude
                long = longitude
                pos.clear()
                pos.add(lat)
                pos.add(long)
                indirizzo = titolo
                println("Tornato dal maps fragment, lat:$lat, long:$long, addr:$indirizzo")
                supportFragmentManager.beginTransaction()
                    .detach(advancedResearchFragment)
                    .commit()

                var bundle = Bundle()
                bundle.putString("code", "1")
                bundle.putString("address", titolo)
                bundle.putDouble("lat", latitude)
                bundle.putDouble("long", longitude)
                advancedResearchFragment.arguments = bundle
                supportFragmentManager.beginTransaction()
                    .remove(mapsFragment)
                    .attach(advancedResearchFragment)
                    .commit()
            }
        }
    }

    override fun openMap() {
        mapsFragment = MapsFragment()
        supportFragmentManager.beginTransaction()
            .add(R.id.main_holder, mapsFragment)
            .attach(mapsFragment)
            .commit()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomePageNewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //toolbar
        drawerLayout = findViewById(R.id.home_drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        val header = navigationView.getHeaderView(0)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        val toggle =
            ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        //inizializzazione database e autenticazione utente
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        loadFragments()
        indirizzo = getString(R.string.base_pos_err)
        //retrival currentUser e setting info user
        MainScope().launch {
            try {
                withContext(Dispatchers.Main) {
                    code = intent.getIntExtra("code", 0)
                    println("Codice : $code")
                    currentUser = User().getCurrentUser(auth!!, db)
                    setUserInfo(header)

                    if (code == 1) {
                        var bundle = Bundle()
                        bundle.putString("code", "1")
                        bundle.putStringArrayList(
                            "searchResult",
                            intent.getStringArrayListExtra("resAr")
                        )
                        advancedResearchFragment.arguments = bundle
                        runOnUiThread {
                            goneDividers()
                            supportFragmentManager.beginTransaction()
                                .setCustomAnimations(
                                    R.anim.slide_in_right,  // Animation for the entering fragment
                                    R.anim.slide_out_left, // Animation for the exiting fragment
                                    R.anim.slide_in_left,  // Animation for the entering fragment (reverse)
                                    R.anim.slide_out_right   // Animation for the exiting fragment (reverse)
                                )
                                .detach(homeFragment)
                                .attach(resultFragment)
                                .commit()
                            current = 3
                            binding.dividerResult.visibility = View.VISIBLE
                        }
                    }
                }
                if (currentUser.Admin) {
                    for (i in 0..navigationView.menu.size() - 1) {
                        val menuitem: MenuItem = navigationView.menu.get(i)
                        if (menuitem.itemId == R.id.nav_sell_item) menuitem.setVisible(false)
                        if (menuitem.itemId == R.id.nav_profile) menuitem.setVisible(false)
                        if (menuitem.itemId == R.id.nav_reviews) menuitem.setVisible(false)
                        if (menuitem.itemId == R.id.nav_Admin_user) menuitem.setVisible(true)
                        if (menuitem.itemId == R.id.nav_Admin_stat) menuitem.setVisible(true)
                    }
                } else if (currentUser.Status == 1) {
                    AlertDialog.Builder(this@HomePage).setTitle(R.string.attenzione)
                        .setMessage(
                            getString(R.string.suspended_allert) +
                                    getString(R.string.suspended_allert1) +
                                    getString(R.string.suspended_allert2)
                        ).setNeutralButton("Ok") { dialog: DialogInterface, _: Int ->
                            dialog.dismiss()
                        }.show()
                } else if (currentUser.Status == 2) {
                    val intent = Intent(this@HomePage, BannedActivity::class.java)
                    startActivity(intent)
                    finish()
                } else if (currentUser.Status == 3) {
                    AlertDialog.Builder(this@HomePage).setTitle(getString(R.string.attenzione))
                        .setMessage(
                            getString(R.string.banned) +
                                    getString(R.string.banned1) +
                                    getString(R.string.banned2)
                        ).setNeutralButton("Ok") { dialog: DialogInterface, _: Int ->
                            dialog.dismiss()
                            CoroutineScope(Dispatchers.IO).launch {
                                delUser()
                            }
                        }.show()
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            this@HomePage,
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            this@HomePage,
                            arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                            123
                        )
                    } else {
                        checkPref()
                        checkSavedSearch()
                    }
                } else {
                    checkPref()
                    checkSavedSearch()
                }
            } catch (e: Exception) {
                println("$e : Exception")
                logout()
            }

        }

        //drawer
        navigationView.setNavigationItemSelectedListener {
            when (it.itemId) {

                R.id.nav_home -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                }

                R.id.nav_chat -> {
                    val intent = Intent(this, ChatActivity::class.java)
                    intent.putExtra("sender", auth!!.currentUser!!.uid)
                    startActivity(intent)
                }

                R.id.nav_about_us-> {
                    val intent = Intent(this, AboutUs::class.java)
                    intent.putExtra("code", 1)
                    startActivity(intent)
                }

                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                }

                R.id.nav_sell_item -> {
                    val intent = Intent(this, SellFormActivity::class.java)
                    startActivity(intent)
                }

                R.id.nav_reviews -> {
                    toggle.isDrawerIndicatorEnabled = false
                    val bundle = Bundle()
                    bundle.putInt("code", 2)
                    resFragment.arguments = bundle
                    val transaction = supportFragmentManager.beginTransaction()
                    transaction.add(R.id.main_holder, resFragment).commit()

                    if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        drawerLayout.closeDrawer(GravityCompat.START)
                    }
                }

                R.id.nav_Admin_user -> {
                    if (currentUser.Admin) {
                        val intent = Intent(this, UserHandleActivity::class.java)
                        startActivity(intent)
                    }
                }

                R.id.nav_Admin_stat -> {
                    if (currentUser.Admin) {
                        val intent = Intent(this, StatisticActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
            true
        }

        binding.homeBtn.setOnClickListener {
            disableBTNS()
            goneDividers()
            if (homeFragment.isDetached) {
                when {
                    advancedResearchFragment.isVisible -> slideToLeft(
                        advancedResearchFragment,
                        homeFragment
                    )

                    resultFragment.isVisible -> slideToLeft(
                        resultFragment,
                        homeFragment
                    )

                    prefFragment.isVisible -> slideToLeft(
                        prefFragment,
                        homeFragment
                    )
                }
            }
            binding.dividerHome.visibility = View.VISIBLE
            enableBTNS()
        }

        binding.advancedResBtn.setOnClickListener {
            disableBTNS()
            goneDividers()
            if (advancedResearchFragment.isDetached) {
                when {
                    homeFragment.isVisible -> slideToRigth(
                        homeFragment,
                        advancedResearchFragment
                    )

                    resultFragment.isVisible -> slideToLeft(
                        resultFragment,
                        advancedResearchFragment
                    )

                    prefFragment.isVisible -> slideToLeft(
                        prefFragment,
                        advancedResearchFragment
                    )
                }
            }
            binding.dividerAdvS.visibility = View.VISIBLE
            enableBTNS()
        }

        binding.prefBtn.setOnClickListener {
            disableBTNS()
            goneDividers()
            if (prefFragment.isDetached) {
                when {
                    homeFragment.isVisible -> slideToRigth(
                        homeFragment,
                        prefFragment
                    )

                    advancedResearchFragment.isVisible -> slideToRigth(
                        advancedResearchFragment,
                        prefFragment
                    )

                    resultFragment.isVisible -> slideToLeft(
                        resultFragment,
                        prefFragment
                    )
                }
            }
            binding.dividerPref.visibility = View.VISIBLE
            enableBTNS()
        }

        binding.resultBtn.setOnClickListener {
            disableBTNS()
            goneDividers()
            if (resultFragment.isDetached) {
                when {
                    homeFragment.isVisible -> slideToRigth(
                        homeFragment,
                        resultFragment
                    )

                    advancedResearchFragment.isVisible -> slideToRigth(
                        advancedResearchFragment,
                        resultFragment
                    )

                    prefFragment.isVisible -> slideToRigth(
                        prefFragment,
                        resultFragment
                    )
                }
            }
            binding.dividerResult.visibility = View.VISIBLE
            enableBTNS()
        }

        binding.logoutBtn.setOnClickListener {
            logout()
        }

        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    if (mapsFragment.isAdded) {
                        supportFragmentManager.beginTransaction()
                            .remove(mapsFragment)
                            .commitNow()
                    } else {
                        finish()
                    }
                }
            }
        }
        onBackPressedDispatcher.addCallback(onBackPressedCallback)

        binding.mainHolder.setOnClickListener {
            if (!mapsFragment.isAdded) {
                it.hideKeyboard()
            }
        }

        binding.searchBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                MainScope().launch {
                    withContext(Dispatchers.Main) {
                        val ids = queryItemsIds(binding.searchBar.text.toString())
                        val bundle = Bundle()
                        bundle.putString("code", "1")
                        bundle.putStringArrayList("searchResult", ids)
                        resultFragment.arguments = bundle
                        disableBTNS()
                        goneDividers()
                        if (resultFragment.isDetached) {
                            when {
                                homeFragment.isVisible -> slideToRigth(
                                    homeFragment,
                                    resultFragment
                                )

                                advancedResearchFragment.isVisible -> slideToRigth(
                                    advancedResearchFragment,
                                    resultFragment
                                )

                                prefFragment.isVisible -> slideToRigth(
                                    prefFragment,
                                    resultFragment
                                )
                            }
                            binding.dividerResult.visibility = View.VISIBLE
                            enableBTNS()
                        } else {
                            supportFragmentManager.beginTransaction()
                                .remove(resultFragment)
                                .commitNow()
                            val bundle = Bundle()
                            bundle.putString("code", "1")
                            bundle.putStringArrayList("searchResult", ids)
                            resultFragment = ResultFragment()
                            resultFragment.arguments = bundle
                            supportFragmentManager.beginTransaction()
                                .add(R.id.fragment_holder, resultFragment)
                                .attach(resultFragment)
                                .commitNow()
                        }
                        binding.dividerResult.visibility = View.VISIBLE
                        enableBTNS()
                    }
                }
                true
            } else {
                false
            }
        }
    }

    private fun disableBTNS() {
        binding.homeBtn.isEnabled = false
        binding.advancedResBtn.isEnabled = false
        binding.prefBtn.isEnabled = false
        binding.resultBtn.isEnabled = false
    }

    private fun enableBTNS() {
        binding.homeBtn.isEnabled = true
        binding.advancedResBtn.isEnabled = true
        binding.prefBtn.isEnabled = true
        binding.resultBtn.isEnabled = true
    }

    private fun slideToRigth(oldFrag: Fragment, newFrag: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .detach(oldFrag)
            .attach(newFrag)
            .commitNow()
    }

    private fun slideToLeft(oldFrag: Fragment, newFrag: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_left,
                R.anim.slide_out_right,
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
            .detach(oldFrag)
            .attach(newFrag)
            .commitNow()
    }


    private fun View.hideKeyboard() {
        val inputManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun goneDividers() {
        binding.dividerHome.visibility = View.GONE
        binding.dividerAdvS.visibility = View.GONE
        binding.dividerPref.visibility = View.GONE
        binding.dividerResult.visibility = View.GONE
    }

    private fun loadFragments() {
        runOnUiThread {
            homeFragment = HomeFragment()
            advancedResearchFragment = AdvancedResearchFragment()
            resultFragment = ResultFragment()
            prefFragment = PrefFragment()

            var bundle = Bundle()
            bundle.putString("code", "0")
            advancedResearchFragment.arguments = bundle
            resultFragment.arguments = bundle

            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_holder, homeFragment)
                .add(R.id.fragment_holder, resultFragment)
                .add(R.id.fragment_holder, advancedResearchFragment)
                .add(R.id.fragment_holder, prefFragment)
                .commitNow()

            supportFragmentManager.beginTransaction()
                .detach(advancedResearchFragment)
                .detach(resultFragment)
                .detach(prefFragment)
                .attach(homeFragment)
                .commitNow()

            println("Fragments set up")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        MainScope().launch {
            if (requestCode == 123) {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkPref()
                    checkSavedSearch()
                } else {
                    Toast.makeText(
                        this@HomePage,
                        getString(R.string.auth_notification_missing), Toast.LENGTH_LONG
                    )
                        .show()
                }
            }
        }
    }

    private suspend fun queryItemsIds(input: String): ArrayList<String> {
        val itemIds = arrayListOf<String>()
        val snapshot = db.collection("Item").whereEqualTo("Venduto", false).get().await()
        for (doc in snapshot) {
            if (doc.get("Proprietario") != currentUser.Username && doc.get("Nome").toString()
                    .lowercase().contains(input.lowercase())
            ) {
                itemIds.add(doc.id)
            }
        }
        println("Total items Retrived : ${itemIds.size}")
        itemIds.sortBy { it }
        return itemIds
    }


    private fun checkPref() {
        for (pref in currentUser.Preferiti) {
            if (pref.value == true) {
                println("Trovato un preferito che ha subito una modifica")
                currentUser.Preferiti[pref.key] = false
                currentUser.updateUserPrefMod(db, auth!!.uid!!)
                notifyPriceModification(pref.key)
            }
        }
    }


    private fun notifyPriceModification(itemID: String) {
        val intent = Intent(this, ItemActivity::class.java)
        intent.putExtra("id", itemID)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)

        createNotificationChannel(
            "it.uniupo.ebayclone",
            "new items",
            getString(R.string.notification_desc1)
        )
        sendNotificationRes(
            "it.uniupo.ebayclone",
            102,
            getString(R.string.notification_title_pref),
            getString(R.string.notification_desc_2),
            pendingIntent
        )
    }


    override fun onResume() {
        super.onResume()
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        refreshUtente()

    }

    private fun refreshUtente() {
        MainScope().launch {
            withContext(Dispatchers.Main) {
                currentUser = User().getCurrentUser(auth!!, db)
                if (currentUser.Admin) {
                    for (i in 0..navigationView.menu.size() - 1) {
                        val menuitem: MenuItem = navigationView.menu.get(i)
                        if (menuitem.itemId == R.id.nav_sell_item) menuitem.setVisible(false)
                        if (menuitem.itemId == R.id.nav_profile) menuitem.setVisible(false)
                        if (menuitem.itemId == R.id.nav_reviews) menuitem.setVisible(false)
                        if (menuitem.itemId == R.id.nav_Admin_user) menuitem.setVisible(true)
                        if (menuitem.itemId == R.id.nav_Admin_stat) menuitem.setVisible(true)
                    }
                } else if (currentUser.Status == 1) {
                    AlertDialog.Builder(this@HomePage).setTitle(R.string.attenzione)
                        .setMessage(
                            getString(R.string.suspended_allert) +
                                    getString(R.string.suspended_allert1) +
                                    getString(R.string.suspended_allert2)
                        ).setNeutralButton("Ok") { dialog: DialogInterface, _: Int ->
                            dialog.dismiss()
                        }.show()
                } else if (currentUser.Status == 2) {
                    val intent = Intent(this@HomePage, BannedActivity::class.java)
                    startActivity(intent)
                    finish()
                } else if (currentUser.Status == 3) {
                    AlertDialog.Builder(this@HomePage).setTitle(getString(R.string.attenzione))
                        .setMessage(
                            getString(R.string.banned) +
                                    getString(R.string.banned1) +
                                    getString(R.string.banned2)
                        ).setNeutralButton("Ok") { dialog: DialogInterface, _: Int ->
                            dialog.dismiss()
                            CoroutineScope(Dispatchers.IO).launch {
                                delUser()
                            }
                        }.show()
                }
            }
        }
    }

    private suspend fun delUser() {
        var user = FirebaseAuth.getInstance().currentUser!!
        //elimino l'utente dal database
        db.collection("User").document(user.uid).delete()
        user.delete().await()
        var intent = Intent(this, FirstActivity::class.java)
        startActivity(intent)
        finish()
    }

    //FUNZIONI SETTIING CURRENT USER
    private fun setUserInfo(header: View) {
        header.findViewById<TextView>(R.id.home_username).text = currentUser.Username
        header.findViewById<TextView>(R.id.home_email).text = currentUser.Email
        binding.tooolbarTitle.text = getString(R.string.home_username) + " ${currentUser.Username}"
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, FirstActivity::class.java))
        Toast.makeText(this, "Logout", Toast.LENGTH_SHORT).show()
        finish()
    }

    private suspend fun checkSavedSearch() {
        if (currentUser.Username == "") {
            logout()
        }
        var notID = 101
        var query = db.collection("Research").whereEqualTo("userID", auth!!.currentUser!!.uid)
        var snapshot = query.get().await()
        if (snapshot!!.isEmpty) {
        }
        for (research in snapshot) {

            val r = research.toObject(Research::class.java)
            //prendo tutti gli oggetti caricati dopo la ricerca
            val q = db.collection("Item").get()
            val s = q.await()
            //result map conterrà tutti gli oggetti che trovo
            var resultMap: HashMap<Item, String>
            var tmpMap = HashMap<Item, String>()
            if (r.searchName.isNullOrEmpty()) {
                for (items in s) {
                    if (items.id > research.id) {
                        val itemFull: Item = items.toObject(Item::class.java)
                        if (itemFull.Proprietario != currentUser.Username) {
                            tmpMap[itemFull] = items.id
                        }
                    }
                }
            } else {
                //recupero solo quelli che hanno il nome
                for (items in s) {
                    if (items.id > research.id) {
                        if (r.searchName.lowercase().trim()
                                .contains(items.get("Nome").toString().trim().lowercase())
                        ) {
                            val itemFull: Item = items.toObject(Item::class.java)
                            if (itemFull.Proprietario != currentUser.Username) {
                                tmpMap[itemFull] = items.id
                            }
                        }
                    }
                }
            }
            println("TMP MAP = ${tmpMap}")
            if (tmpMap.isNotEmpty()) {
                resultMap = researchResultHP(r, tmpMap)
                println("Ricerca finita, resultMap : ${resultMap},\nsize = ${resultMap.size}")
                if (resultMap.isNotEmpty()) {
                    var resAr: ArrayList<String> = getItemId(resultMap)
                    val intent = Intent(this, HomePage::class.java).apply {
                        putExtra("code", 1)
                        putStringArrayListExtra("resAr", resAr)
                    }
                    println("size resAr = ${resAr.size}")
                    val pendingIntent =
                        PendingIntent.getActivity(
                            this,
                            0,
                            intent,
                            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    createNotificationChannel(
                        "it.uniupo.ebayclone",
                        "new items",
                        getString(R.string.notification_desc3)
                    )
                    sendNotificationRes(
                        "it.uniupo.ebayclone",
                        notID,
                        getString(R.string.notificatiion_title_search),
                        getString(R.string.notification_dec4),
                        pendingIntent
                    )
                    notID++
                    //aggionro l'id della ricerca salvata
                    db.collection("Research").document(research.id).delete()
                    r.saveSearch(r, db)
                }
            }
        }
    }

    private fun getItemId(map: HashMap<Item, String>): ArrayList<String> {
        val res = arrayListOf<String>()
        for (items in map) {
            res.add(items.value)
        }
        for (id in res) {
            println(id)
        }
        return res
    }

    private fun researchResultHP(
        research: Research,
        startMap: HashMap<Item, String>
    ): HashMap<Item, String> {

        println("Ricerca partita per notifica")
        var resMap: HashMap<Item, String>
        var tmpMap: HashMap<Item, String>
        tmpMap = research.queryShipping(startMap)
        println("Ricerca shipping finita, risultato : ${tmpMap}")
        if (tmpMap.isEmpty()) {
            println("Torno vuoto")
            return hashMapOf()
        } else {
            resMap = tmpMap.clone() as HashMap<Item, String>
        }
        tmpMap.clear()
        if (research.searchPos?.isNotEmpty() == true && research.searchDistance != -1.0) {
            tmpMap = research.queryPos(resMap)
            println("Ricerca posizione finita, risultato : ${tmpMap}")
            if (tmpMap.isEmpty()) {
                println("Torno vuoto")
                return hashMapOf()
            } else {
                resMap = tmpMap.clone() as HashMap<Item, String>
            }
        }

        tmpMap.clear()
        if (research.searchPriceMin?.isNotEmpty() == true || research.searchPriceMax?.isNotEmpty() == true) {
            tmpMap = research.queryPrice(resMap)
            println("Ricerca prezzo finita, risultato : ${tmpMap}")
            if (tmpMap.isEmpty()) {
                println("Torno vuoto")
                return hashMapOf()
            } else {
                resMap = tmpMap.clone() as HashMap<Item, String>
            }
        }
        return resMap
    }

    private fun createNotificationChannel(id: String, name: String, description: String) {
        println("Channel notification created")
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(id, name, importance).apply {
            this.description = description
        }
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun sendNotificationRes(
        channelId: String,
        notID: Int,
        title: String,
        desc: String,
        intent: PendingIntent?
    ) {
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.sfondo_app)
            .setContentTitle(title)
            .setContentText(desc)
            .setContentIntent(intent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(notID, notificationBuilder.build())
        println("notifica mandata")
    }


}








