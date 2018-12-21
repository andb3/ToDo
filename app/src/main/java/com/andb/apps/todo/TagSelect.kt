package com.andb.apps.todo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.andb.apps.todo.eventbus.AddTaskAddTagEvent
import com.andb.apps.todo.filtering.Filters
import com.andb.apps.todo.lists.TagLinkList
import com.andb.apps.todo.lists.TagList
import com.andb.apps.todo.lists.interfaces.LinkListInterface
import com.andb.apps.todo.lists.interfaces.TagListInterface
import com.andrognito.flashbar.Flashbar
import com.github.rongi.klaster.Klaster
import com.jaredrummler.cyanea.Cyanea
import com.jaredrummler.cyanea.app.CyaneaAppCompatActivity
import kotlinx.android.synthetic.main.content_tag_select.*
import kotlinx.android.synthetic.main.tag_list_item.*
import kotlinx.android.synthetic.main.tag_list_item.view.*
import org.greenrobot.eventbus.EventBus


class TagSelect : CyaneaAppCompatActivity() {



    private var isTaskCreate = false

    private var isTagLink = false


    private var contextualToolbar: ActionMode? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag_select)
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)


        val bundle = intent.extras
        if (bundle!!.containsKey("isTaskCreate")) {
            isTaskCreate = bundle.getBoolean("isTaskCreate")
        }

        if (intent.hasExtra("isTagLink")) {
            isTagLink = bundle.getBoolean("isTagLink")

        }

        var tagRecycler = tagrecycler
        tagRecycler.layoutManager = LinearLayoutManager(this)


        // specify an adapter (see also next example)
        mAdapter = tagAdapter()
        tagRecycler.adapter = mAdapter


        tagRecycler.addOnItemTouchListener(RecyclerTouchListener(applicationContext, tagRecycler, object : RecyclerTouchListener.ClickListener {
            override fun onClick(view: View, position: Int) {
                val tags = TagList.getItem(position)
                Toast.makeText(applicationContext, tags.tagName + " is selected!", Toast.LENGTH_SHORT).show()


                if (isTaskCreate) {

                    EventBus.getDefault().post(AddTaskAddTagEvent(position))
                    finish()
                } else if (isTagLink && Filters.getCurrentFilter().size > 0) {
                    val finish = LinkListInterface.addLinkToCurrentDirectory(position, this@TagSelect)
                    if (finish) {
                        finish()
                    }
                } else {
                    Filters.tagForward(position)
                    finish()
                }

            }

            override fun onLongClick(view: View, position: Int) {

                contextualToolbar = this@TagSelect.startActionMode(setCallback(position))
                view.isSelected = true
            }


        }))


    }

    private fun tagAdapter() = Klaster.get()
            .itemCount { TagList.tagList.size }
            .view(R.layout.tag_list_item, layoutInflater)
            .bind { position ->
                val tag = TagList.tagList[position]
                itemView.tagname.text = tag.tagName
                itemView.tagIcon.setColorFilter(tag.tagColor)
            }
            .build()


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_tag_select, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.app_bar_add -> {
                val editTask = Intent(this, CreateTag::class.java)
                editTask.putExtra("edit", false)
                startActivity(editTask)
                return true
            }
            else ->
                return super.onOptionsItemSelected(item)
        }
    }


    fun setCallback(position: Int): ActionMode.Callback {
        return object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                val menuInflater = MenuInflater(this@TagSelect.applicationContext)
                menuInflater.inflate(R.menu.toolbar_tag_select_long_press, menu)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                return false
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                when (item.itemId) {
                    R.id.editTag -> {
                        val editTask = Intent(this@TagSelect.applicationContext, CreateTag::class.java)
                        editTask.putExtra("edit", true)
                        editTask.putExtra("editPos", position)
                        startActivity(editTask)
                        mode.finish()
                        return true
                    }
                    R.id.deleteTag -> {
                        TagListInterface.deleteTag(position, applicationContext)
                        mAdapter.notifyItemRemoved(position)
                        mode.finish()
                    }
                }

                return false
            }

            override fun onDestroyActionMode(mode: ActionMode) {}
        }

    }


    public override fun onPause() {
        super.onPause()
        if (isTagLink) {
            TagLinkList.saveTags(this)
        }
    }


    companion object {
        lateinit var mAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>

        fun tagExists(activity: Activity): Flashbar {
            return Flashbar.Builder(activity)
                    .gravity(Flashbar.Gravity.BOTTOM)
                    .title("Tag Exists")
                    .message("The tag you selected has already been linked")
                    .dismissOnTapOutside()
                    .backgroundColor(Cyanea.instance.accent)
                    .build()

        }

        fun sameTag(activity: Activity): Flashbar {
            return Flashbar.Builder(activity)
                    .gravity(Flashbar.Gravity.BOTTOM)
                    .title("Same Tag")
                    .message("The tag you selected is the current tag. Why would you do that?")
                    .dismissOnTapOutside()
                    .backgroundColor(Cyanea.instance.accent)
                    .build()

        }
    }


}