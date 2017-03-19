package ve.com.abicelis.remindy.app.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;
import com.codetroopers.betterpickers.calendardatepicker.MonthAdapter;
import com.codetroopers.betterpickers.radialtimepicker.RadialTimePickerDialogFragment;
import com.transitionseverywhere.TransitionManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import ve.com.abicelis.remindy.R;
import ve.com.abicelis.remindy.database.RemindyDAO;
import ve.com.abicelis.remindy.enums.ReminderCategory;
import ve.com.abicelis.remindy.enums.ReminderRepeatEndType;
import ve.com.abicelis.remindy.enums.ReminderRepeatType;
import ve.com.abicelis.remindy.enums.ReminderStatus;
import ve.com.abicelis.remindy.exception.CouldNotInsertDataException;
import ve.com.abicelis.remindy.model.SimpleReminder;
import ve.com.abicelis.remindy.model.Time;
import ve.com.abicelis.remindy.util.InputFilterMinMax;
import ve.com.abicelis.remindy.util.SnackbarUtil;

/**
 * Created by abice on 16/3/2017.
 */

public class SimpleReminderActivity extends AppCompatActivity {

    //CONST
    final Calendar mToday = Calendar.getInstance();
    final Calendar mTomorrow = Calendar.getInstance();
    public static final String ARG_SIMPLE_REMINDER = "ARG_SIMPLE_REMINDER";
    public static final String KEY_INSTANCE_STATE_SIMPLE_REMINDER = "KEY_INSTANCE_STATE_SIMPLE_REMINDER";

    //DATA
    private List<String> reminderCategories;
    private List<String> reminderRepeatTypes;
    private List<String> reminderRepeatEndTypes;
    private boolean reminderRepeatActive = false;
    Calendar mDateCal;
    Time mTimeTime;
    Calendar mRepeatUntilCal;
    SimpleReminder mNewReminder = null;


    //UI
    private Toolbar mToolbar;
    private EditText mTitle;
    private EditText mDescription;
    private Spinner mCategory;
    private EditText mDate;
    private EditText mTime;
    private Spinner mRepeatType;
    private LinearLayout mTransitionsContainer;
    private LinearLayout mRepeatContainer;
    private EditText mRepeatInterval;
    private TextView mRepeatTypeTitle;
    private LinearLayout mRepeatEndForEventsContainer;
    private LinearLayout mRepeatEndUntilContainer;
    private EditText mRepeatEndForXEvents;
    private Spinner mRepeatEndType;
    private EditText mRepeatUntilDate;
    private CalendarDatePickerDialogFragment mDatePicker;
    private CalendarDatePickerDialogFragment mRepeatUntilDatePicker;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_reminder);
        Log.d("SUPERTAG", "onCreate");


        mToolbar = (Toolbar) findViewById(R.id.activity_reminder_simple_toolbar);
        mTitle = (EditText) findViewById(R.id.activity_reminder_simple_title);
        mDescription = (EditText) findViewById(R.id.activity_reminder_simple_description);
        mCategory = (Spinner) findViewById(R.id.activity_reminder_simple_category);
        mDate = (EditText) findViewById(R.id.activity_reminder_simple_date);
        mTime = (EditText) findViewById(R.id.activity_reminder_simple_time);
        mRepeatType = (Spinner) findViewById(R.id.activity_reminder_simple_repeat_type);
        mTransitionsContainer = (LinearLayout) findViewById(R.id.activity_reminder_simple_transitions_container);
        mRepeatContainer = (LinearLayout) findViewById(R.id.activity_reminder_simple_repeat_container);
        mRepeatInterval = (EditText) findViewById(R.id.activity_reminder_simple_repeat_interval);
        mRepeatTypeTitle = (TextView) findViewById(R.id.activity_reminder_simple_repeat_type_title);
        mRepeatEndForEventsContainer = (LinearLayout) findViewById(R.id.activity_reminder_simple_repeat_end_for_events_container);
        mRepeatEndUntilContainer = (LinearLayout) findViewById(R.id.activity_reminder_simple_repeat_end_until_container);
        mRepeatEndForXEvents = (EditText) findViewById(R.id.activity_reminder_simple_repeat_for_x_events);
        mRepeatEndType = (Spinner) findViewById(R.id.activity_reminder_simple_repeat_end_type);
        mRepeatUntilDate = (EditText) findViewById(R.id.activity_reminder_simple_repeat_until);

        mRepeatInterval.setFilters(new InputFilter[]{new InputFilterMinMax("1", "99")});
        mRepeatEndForXEvents.setFilters(new InputFilter[]{new InputFilterMinMax("1", "99")});

        mToolbar.setTitle(R.string.activity_reminder_toolbar_title);
        mToolbar.setNavigationIcon(ContextCompat.getDrawable(this, R.drawable.icon_back_material));
        setSupportActionBar(mToolbar);

        //If activity was called to edit an existing reminder, check ARG_SIMPLE_REMINDER
        if(getIntent().hasExtra(ARG_SIMPLE_REMINDER)) {
            mNewReminder = (SimpleReminder) getIntent().getSerializableExtra(ARG_SIMPLE_REMINDER);
            restoreSimpleReminder();
        }

        //If screen was turned, restore state!
        if(savedInstanceState != null && savedInstanceState.containsKey(KEY_INSTANCE_STATE_SIMPLE_REMINDER)) {
            mNewReminder = (SimpleReminder) savedInstanceState.getSerializable(KEY_INSTANCE_STATE_SIMPLE_REMINDER);
            restoreSimpleReminder();
        }

        //Add a day to mTomorrow cal
        mTomorrow.add(Calendar.DAY_OF_MONTH, 1);

        setupSpinners();
        setupDateAndTimePickers();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //Screen was turned, save the state before killing the activity
        saveSimpleReminder();
        outState.putSerializable(KEY_INSTANCE_STATE_SIMPLE_REMINDER, mNewReminder);
    }

    @Override
    public void onBackPressed() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.activity_reminder_simple_exit_dialog_title))
                .setMessage(getResources().getString(R.string.activity_reminder_simple_exit_dialog_message))
                .setPositiveButton(getResources().getString(R.string.activity_reminder_simple_exit_dialog_positive),  new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                })
                .setNegativeButton(getResources().getString(R.string.activity_reminder_simple_exit_dialog_negative), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
        dialog.show();
    }

    private void setupSpinners() {
        reminderCategories = ReminderCategory.getFriendlyValues(this);
        ArrayAdapter reminderCategoryAdapter = new ArrayAdapter<String>(this, R.layout.spinner_item, reminderCategories);
        reminderCategoryAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        mCategory.setAdapter(reminderCategoryAdapter);

        reminderRepeatTypes = ReminderRepeatType.getFriendlyValues(this);
        ArrayAdapter reminderRepeatTypeAdapter = new ArrayAdapter<String>(this, R.layout.spinner_item, reminderRepeatTypes);
        reminderRepeatTypeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        mRepeatType.setAdapter(reminderRepeatTypeAdapter);
        mRepeatType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                handleRepeatTypeSelected(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        reminderRepeatEndTypes = ReminderRepeatEndType.getFriendlyValues(this);
        ArrayAdapter reminderRepeatEndTypeAdapter = new ArrayAdapter<String>(this, R.layout.spinner_item, reminderRepeatEndTypes);
        reminderRepeatEndTypeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        mRepeatEndType.setAdapter(reminderRepeatEndTypeAdapter);
        mRepeatEndType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                handleRepeatEndTypeSelected(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupDateAndTimePickers() {

        mDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDatePicker = new CalendarDatePickerDialogFragment()
                        .setOnDateSetListener(new CalendarDatePickerDialogFragment.OnDateSetListener() {
                            @Override
                            public void onDateSet(CalendarDatePickerDialogFragment dialog, int year, int monthOfYear, int dayOfMonth) {
                                if(mDateCal == null) {
                                    mDateCal = Calendar.getInstance();
                                    mDateCal.set(Calendar.HOUR_OF_DAY, 0);
                                    mDateCal.set(Calendar.MINUTE, 0);
                                    mDateCal.set(Calendar.SECOND, 0);
                                    mDateCal.set(Calendar.MILLISECOND, 0);
                                }
                                mDateCal.set(year, monthOfYear, dayOfMonth);
                                SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                                mDate.setText(formatter.format(mDateCal.getTime()));
                            }
                        })
                        .setFirstDayOfWeek(Calendar.MONDAY)
                        .setPreselectedDate(mToday.get(Calendar.YEAR), mToday.get(Calendar.MONTH), mToday.get(Calendar.DAY_OF_MONTH))
                        .setDateRange(new MonthAdapter.CalendarDay(mToday), null)
                        .setDoneText(getResources().getString(R.string.datepicker_ok))
                        .setCancelText(getResources().getString(R.string.datepicker_cancel));
                mDatePicker.show(getSupportFragmentManager(), "mDate");
            }
        });

        mRepeatUntilDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRepeatUntilDatePicker = new CalendarDatePickerDialogFragment()
                        .setOnDateSetListener(new CalendarDatePickerDialogFragment.OnDateSetListener() {
                            @Override
                            public void onDateSet(CalendarDatePickerDialogFragment dialog, int year, int monthOfYear, int dayOfMonth) {
                                if(mRepeatUntilCal == null) {
                                    mRepeatUntilCal = Calendar.getInstance();
                                    mRepeatUntilCal.set(Calendar.HOUR_OF_DAY, 0);
                                    mRepeatUntilCal.set(Calendar.MINUTE, 0);
                                    mRepeatUntilCal.set(Calendar.SECOND, 0);
                                    mRepeatUntilCal.set(Calendar.MILLISECOND, 0);
                                }
                                mRepeatUntilCal.set(year, monthOfYear, dayOfMonth);
                                SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                                mRepeatUntilDate.setText(formatter.format(mRepeatUntilCal.getTime()));
                            }
                        })
                        .setFirstDayOfWeek(Calendar.MONDAY)
                        .setPreselectedDate(mTomorrow.get(Calendar.YEAR), mTomorrow.get(Calendar.MONTH), mTomorrow.get(Calendar.DAY_OF_MONTH))
                        .setDateRange(new MonthAdapter.CalendarDay(mTomorrow), null)
                        .setDoneText(getResources().getString(R.string.datepicker_ok))
                        .setCancelText(getResources().getString(R.string.datepicker_cancel));
                mRepeatUntilDatePicker.show(getSupportFragmentManager(), "mRepeatUntilDate");
            }
        });

        mTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RadialTimePickerDialogFragment rtpd = new RadialTimePickerDialogFragment()
                        .setOnTimeSetListener(new RadialTimePickerDialogFragment.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(RadialTimePickerDialogFragment dialog, int hourOfDay, int minute) {
                                if(mTimeTime == null) {
                                    mTimeTime = new Time();
                                    //TODO: grab timeFormat from preferences and mTimeTime.setDisplayTimeFormat();

                                }
                                mTimeTime.setHour(hourOfDay);
                                mTimeTime.setMinute(minute);
                                mTime.setText(mTimeTime.toString());
                            }
                        })
                        .setStartTime(12, 0)
                        .setDoneText(getResources().getString(R.string.datepicker_ok))
                        .setCancelText(getResources().getString(R.string.datepicker_cancel));
                rtpd.show(getSupportFragmentManager(), "mTime");
            }
        });
    }



    private void handleRepeatTypeSelected(int position) {

//                TransitionSet set = new TransitionSet()
//                        .addTransition(new Scale(0.9f))
//                        .addTransition(new Fade())
//                        .setInterpolator(reminderRepeatActive ? new FastOutLinearInInterpolator() :
//                                new FastOutLinearInInterpolator());

        if(ReminderRepeatType.values()[position] != ReminderRepeatType.DISABLED && !reminderRepeatActive) {
            TransitionManager.beginDelayedTransition(mTransitionsContainer);
            mRepeatContainer.setVisibility(View.VISIBLE);
            reminderRepeatActive = true;

        } else if(ReminderRepeatType.values()[position] == ReminderRepeatType.DISABLED && reminderRepeatActive) {
            TransitionManager.beginDelayedTransition(mTransitionsContainer);
            mRepeatContainer.setVisibility(View.INVISIBLE);
            reminderRepeatActive = false;
        }

        switch(ReminderRepeatType.values()[position]) {
            case DAILY:
                mRepeatTypeTitle.setText(R.string.activity_reminder_simple_repeat_interval_days);
                break;
            case WEEKLY:
                mRepeatTypeTitle.setText(R.string.activity_reminder_simple_repeat_interval_weeks);
                break;
            case MONTHLY:
                mRepeatTypeTitle.setText(R.string.activity_reminder_simple_repeat_interval_months);
                break;
            case YEARLY:
                mRepeatTypeTitle.setText(R.string.activity_reminder_simple_repeat_interval_years);
                break;
        }

        if(ReminderRepeatType.values()[position] != ReminderRepeatType.DISABLED) {
            mRepeatInterval.requestFocus();
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

    }

    private void handleRepeatEndTypeSelected(int position) {
        switch (ReminderRepeatEndType.values()[position]) {
            case FOREVER:
                TransitionManager.beginDelayedTransition(mTransitionsContainer);
                mRepeatEndForEventsContainer.setVisibility(View.GONE);
                mRepeatEndUntilContainer.setVisibility(View.GONE);
                break;

            case UNTIL_DATE:
                TransitionManager.beginDelayedTransition(mTransitionsContainer);
                mRepeatEndForEventsContainer.setVisibility(View.GONE);
                mRepeatEndUntilContainer.setVisibility(View.VISIBLE);
                break;

            case FOR_X_EVENTS:
                TransitionManager.beginDelayedTransition(mTransitionsContainer);
                mRepeatEndForEventsContainer.setVisibility(View.VISIBLE);
                mRepeatEndUntilContainer.setVisibility(View.GONE);
                mRepeatEndForXEvents.requestFocus();
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                break;
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_simple_reminder, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch(id) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_add_extras:
                saveSimpleReminder();
                //TODO: startActivityForResult() a AddExtrasActivity() and get extras.
                //Recover them in onActivityResult() bundle
                //Also restore reminder data into form
                //Add a little number on extras menu icon to indicate extras have been added?
                break;
            case R.id.action_save:
                if(valuesAreGood()) {
                    saveSimpleReminder();
                    RemindyDAO dao = new RemindyDAO(this);
                    try {
                        dao.insertSimpleReminder(mNewReminder);


                        SnackbarUtil.showSuccessSnackbar(mRepeatContainer, R.string.reminder_saved_successfully);

                    } catch (CouldNotInsertDataException e) {
                        SnackbarUtil.showErrorSnackbar(mRepeatContainer, R.string.error_problem_inserting_reminder);
                    }
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean valuesAreGood() {
        String title = mTitle.getText().toString();
        if(title.trim().isEmpty()) {
            SnackbarUtil.showErrorSnackbar(mRepeatContainer, R.string.error_invalid_title);
            return false;
        }

        if(mDateCal == null) {
            SnackbarUtil.showErrorSnackbar(mRepeatContainer, R.string.error_invalid_date);
            return false;
        }
        if(mTimeTime == null) {
            SnackbarUtil.showErrorSnackbar(mRepeatContainer, R.string.error_invalid_time);
            mTime.requestFocus();
            return false;
        }

        if(ReminderRepeatType.values()[mRepeatType.getSelectedItemPosition()] != ReminderRepeatType.DISABLED) {
            if(mRepeatInterval.getText().toString().isEmpty()){
                SnackbarUtil.showErrorSnackbar(mRepeatContainer, R.string.error_invalid_repeat_interval);
                return false;
            }

            if(ReminderRepeatEndType.values()[mRepeatEndType.getSelectedItemPosition()] == ReminderRepeatEndType.FOR_X_EVENTS) {
                if(mRepeatEndForXEvents.getText().toString().isEmpty()){
                    SnackbarUtil.showErrorSnackbar(mRepeatContainer, R.string.error_invalid_repeat_events);
                    return false;
                }
            }

            if(ReminderRepeatEndType.values()[mRepeatEndType.getSelectedItemPosition()] == ReminderRepeatEndType.UNTIL_DATE) {
                if(mRepeatUntilCal == null) {
                    SnackbarUtil.showErrorSnackbar(mRepeatContainer, R.string.error_invalid_repeat_until_date);
                    return false;
                }
                if(mRepeatUntilCal.compareTo(mDateCal) <= 0) {
                    SnackbarUtil.showErrorSnackbar(mRepeatContainer, R.string.error_repeat_until_date_after_reminder_date);
                    return false;
                }
            }

        }

        return true;
    }

    private void saveSimpleReminder() {
        String title = mTitle.getText().toString();
        String description = mDescription.getText().toString();
        ReminderCategory category = ReminderCategory.values()[mCategory.getSelectedItemPosition()];
        ReminderRepeatType repeatType = ReminderRepeatType.values()[mRepeatType.getSelectedItemPosition()];

        int repeatInterval = 0;
        ReminderRepeatEndType repeatEndType = null;
        int repeatEndNumberOfEvents = 0;

        if(repeatType != ReminderRepeatType.DISABLED) {
            repeatInterval = Integer.parseInt(mRepeatInterval.getText().toString());
            repeatEndType = ReminderRepeatEndType.values()[mRepeatEndType.getSelectedItemPosition()];

            if(repeatEndType == ReminderRepeatEndType.FOR_X_EVENTS)
                repeatEndNumberOfEvents = Integer.parseInt(mRepeatEndForXEvents.getText().toString());

            if(repeatEndType != ReminderRepeatEndType.UNTIL_DATE)
                mRepeatUntilCal = null;
        }
        mNewReminder = new SimpleReminder(ReminderStatus.ACTIVE, title, description, category, mDateCal, mTimeTime, repeatType, repeatInterval, repeatEndType, repeatEndNumberOfEvents, mRepeatUntilCal);
    }

    private void restoreSimpleReminder() {
        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());


        mTitle.setText(mNewReminder.getTitle());
        mDescription.setText(mNewReminder.getDescription());

        if(mNewReminder.getDate() != null) {
            mDateCal = Calendar.getInstance();
            mDateCal.setTimeInMillis(mNewReminder.getDate().getTimeInMillis());
            mDate.setText(formatter.format(mDateCal.getTime()));
        }

        if(mNewReminder.getTime() != null) {
            mTime.setText(mNewReminder.getTime().toString());
            mTimeTime = new Time(mNewReminder.getTime().getTimeInMinutes());
        }

        mCategory.setSelection(mNewReminder.getCategory().ordinal());
        mRepeatType.setSelection(mNewReminder.getRepeatType().ordinal());


        if(mNewReminder.getRepeatType() != ReminderRepeatType.DISABLED) {
            mRepeatInterval.setText(String.valueOf(mNewReminder.getRepeatInterval()));
            mRepeatEndType.setSelection(mNewReminder.getRepeatEndType().ordinal());
            handleRepeatTypeSelected(mNewReminder.getRepeatType().ordinal());

            if(mNewReminder.getRepeatEndType() == ReminderRepeatEndType.FOR_X_EVENTS)
                mRepeatEndForXEvents.setText(String.valueOf(mNewReminder.getRepeatEndNumberOfEvents()));

            if(mNewReminder.getRepeatEndType() == ReminderRepeatEndType.UNTIL_DATE) {
                if(mNewReminder.getRepeatEndDate() != null) {
                    mRepeatUntilCal = Calendar.getInstance();
                    mRepeatUntilCal.setTimeInMillis(mNewReminder.getRepeatEndDate().getTimeInMillis());
                    mRepeatUntilDate.setText(formatter.format(mRepeatUntilCal.getTime()));
                }
            }

            if(mNewReminder.getRepeatEndType() != ReminderRepeatEndType.FOREVER) {
                handleRepeatEndTypeSelected(mNewReminder.getRepeatEndType().ordinal());

            }

        }
    }

}