package com.andb.apps.todo.ui.addtask

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.location.Geocoder
import android.os.AsyncTask
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.andb.apps.todo.R
import com.andb.apps.todo.notifications.removeFence
import com.andb.apps.todo.notifications.requestFromFence
import com.andb.apps.todo.data.model.Task
import com.andb.apps.todo.data.model.reminders.LocationFence
import com.andb.apps.todo.data.model.reminders.SimpleReminder
import com.andb.apps.todo.util.cyanea.CyaneaDialog
import com.andb.apps.todo.utilities.Current
import com.andb.apps.todo.utilities.ProjectsUtils
import com.andb.apps.todo.utilities.Utilities
import com.github.rongi.klaster.Klaster
import com.jaredrummler.cyanea.Cyanea
import kotlinx.android.synthetic.main.view_reminder_picker.view.*
import kotlinx.android.synthetic.main.view_reminder_picker_item.view.*
import org.joda.time.DateTime
import org.joda.time.LocalTime

const val REMINDER_TIME_TYPE = 45988
const val REMINDER_LOCATION_TYPE = 54374


class ReminderPicker(context: Context) : ConstraintLayout(context) {

    lateinit var task: Task
    private val reminderAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder> by lazy { reminderAdapter() }
    lateinit var dialog: AlertDialog

    init {
        inflate(context, R.layout.view_reminder_picker, this)
    }

    fun setup(task: Task, alertDialog: AlertDialog) {
        this.task = task
        reminderPickerTitle.setBackgroundColor(Utilities.lighterDarker(Cyanea.instance.backgroundColor, .95f))
        reminderPickerRecycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = reminderAdapter
        }
        reminderAddLocationIcon.setOnClickListener {
            pickLocation()
        }
        reminderAddTimeIcon.setOnClickListener {
            pickDateTime()
        }
        reminderPickerConfirm.setOnClickListener {
            alertDialog.cancel()
        }

        val handler = Handler()
        Thread {
            handler.post {

                for(fence in task.locationReminders){
                    val addresses = Geocoder(context).getFromLocation(fence.lat, fence.long, 1)

                    if(fence.name.isEmpty()){
                        fence.name = addresses.let { addresses ->
                            if (addresses.isEmpty()) {
                                context.getString(R.string.location_no_address)
                            } else {
                                val address = addresses[0]
                                val streetAddr = address.getAddressLine(0)
                                streetAddr
                            }
                        }
                    }
                }

                reminderAdapter.notifyDataSetChanged()

            }
        }.start()

    }

    fun reminderAdapter() = Klaster.get()
        .itemCount { task.timeReminders.size + task.locationReminders.size }
        .view(R.layout.view_reminder_picker_item, LayoutInflater.from(context))
        .bind { position ->
            itemView.apply {
                when (itemViewType) {
                    REMINDER_TIME_TYPE -> {
                        val reminder = task.timeReminders[position]
                        setIcon(this, true)
                        reminderItemInfo.text = reminder.toString()
                        reminderItemDelete.setOnClickListener {
                            task.timeReminders.remove(reminder)
                            reminderAdapter.notifyItemRemoved(position)
                        }
                    }
                    REMINDER_LOCATION_TYPE -> {
                        val reminder = task.locationReminders[position - task.timeReminders.size]
                        setIcon(this, false)
                        reminderItemInfo.text = reminder.toString()
                        reminderItemDelete.setOnClickListener {
                            task.locationReminders.remove(reminder)
                            removeFence(reminder.key, context)
                            reminderAdapter.notifyItemRemoved(position)
                        }
                    }
                }
            }
        }
        .getItemViewType { position -> if (position < task.timeReminders.size) REMINDER_TIME_TYPE else REMINDER_LOCATION_TYPE }
        .build()

    fun setIcon(view: View, time: Boolean) {
        view.reminderItemIconTime.visibility = if (time) View.VISIBLE else View.GONE
        view.reminderItemIconLocation.visibility = if (time) View.GONE else View.VISIBLE
    }

    private fun pickLocation() {
        val view = LocationPicker(context)
        dialog = AlertDialog.Builder(context).setView(view).show().also {
            it.window?.setBackgroundDrawable(null)
            it.setOnCancelListener {
                reminderAdapter.notifyDataSetChanged()
            }
        }
        view.setup(dialog.onSaveInstanceState(), ::returnLocation)
    }

    fun returnLocation(fence: LocationFence) {
        dialog.cancel()
        task.locationReminders.add(fence)
        val handler = Handler()

        Thread {
            val addresses = Geocoder(context).getFromLocation(fence.lat, fence.long, 1)
            handler.post {
                fence.name = addresses.let { addresses ->
                    if (addresses.isEmpty()) {
                        context.getString(R.string.location_no_address)
                    } else {
                        val address = addresses[0]
                        val streetAddr = address.getAddressLine(0)
                        streetAddr
                    }
                }

                reminderAdapter.notifyDataSetChanged()

            }
        }.start()

        AsyncTask.execute {
            ProjectsUtils.update(task, async = false)
            val allTasks = Current.taskListAll().filter { !it.isArchived }
            Log.d("fenceTaskAdding", "database search size: ${allTasks.size}")
            Log.d("fenceTaskAdding", "database contains: ${allTasks.contains(task)}")
            Log.d("fenceTaskAdding", "task ids: ${task.locationReminders.map { it.key }}")
            Log.d("fenceTaskAdding", "all reminder ids: ${allTasks.flatMap { it.locationReminders.map { it.key } } }")
            Log.d("fenceTaskAdding", "all trigger ids: ${allTasks.flatMap { it.locationReminders.mapNotNull { it.trigger?.key } } }")
            requestFromFence(fence, context)
        }
    }

    private fun pickDateTime(reminder: SimpleReminder? = null) {
        Log.d("picking date time", "picking date time")
        val currentDate = reminder?.asDateTime() ?: DateTime.now()

        val dialog = DatePickerDialog(context, DatePickerDialog.OnDateSetListener { datePicker, i, i1, i2 ->
            val newDate = currentDate.withDate(i, i1 + 1, i2)
            pickTime(newDate)
        }, currentDate.year, currentDate.monthOfYear - 1, currentDate.dayOfMonth)
        dialog.show()
        CyaneaDialog.setButtonStyle(dialog, DatePickerDialog.BUTTON_NEGATIVE, DatePickerDialog.BUTTON_POSITIVE)
    }

    private fun pickTime(dateTime: DateTime) {
        val timePickerDialog = TimePickerDialog(context, TimePickerDialog.OnTimeSetListener { timePicker, i, i1 ->
            val localTime = LocalTime().withHourOfDay(i).withMinuteOfHour(i1).withSecondOfMinute(0)
            task.timeReminders.add(SimpleReminder(dateTime.withTime(localTime)))
            reminderAdapter.notifyDataSetChanged()
            Log.d("pickTime", "Reminders: ${task.timeReminders.size}")
        }, dateTime.hourOfDay, dateTime.minuteOfHour, false)
        timePickerDialog.show()
        CyaneaDialog.setButtonStyle(timePickerDialog, TimePickerDialog.BUTTON_NEGATIVE, TimePickerDialog.BUTTON_POSITIVE)
    }
}