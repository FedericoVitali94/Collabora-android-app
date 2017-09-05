package org.gammf.collabora_android.app.gui;


import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.location.places.ui.SupportPlaceAutocompleteFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.gammf.collabora_android.app.R;
import org.gammf.collabora_android.app.gui.map.MapManager;
import org.gammf.collabora_android.app.rabbitmq.SendMessageToServerTask;
import org.gammf.collabora_android.app.utils.Observer;
import org.gammf.collabora_android.collaborations.general.Collaboration;
import org.gammf.collabora_android.communication.update.general.UpdateMessageType;
import org.gammf.collabora_android.communication.update.notes.ConcreteNoteUpdateMessage;
import org.gammf.collabora_android.notes.Location;
import org.gammf.collabora_android.notes.Note;
import org.gammf.collabora_android.notes.NoteLocation;
import org.gammf.collabora_android.notes.NoteState;
import org.gammf.collabora_android.utils.LocalStorageUtils;
import org.joda.time.DateTime;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link EditNoteFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class EditNoteFragment extends Fragment implements AdapterView.OnItemSelectedListener,
        DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener, Observer<Location> {

    private static final String ERR_STATENOTSELECTED = "Please select state";

    private static final String ARG_USERNAME = "username";
    private static final String ARG_COLLABID = "collabId";
    private static final String ARG_NOTEID = "noteId";

    private String username;
    private String noteStateEdited = "";
    private TextView dateViewEdited, timeViewEdited;
    private EditText txtContentNoteEdited;
    private Spinner spinnerEditState;
    private DatePickerDialog.OnDateSetListener myDateListenerEdited;
    private TimePickerDialog.OnTimeSetListener myTimeListenerEdited;

    private String collaborationId, noteId;

    private MapManager mapManager;

    private Note note;
    private int year, month, day, hour, minute;

    public EditNoteFragment() {
        setHasOptionsMenu(true);
    }


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment EditNoteFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static EditNoteFragment newInstance(String username, String collabId, String noteId) {
        EditNoteFragment fragment = new EditNoteFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USERNAME, username);
        args.putString(ARG_COLLABID, collabId);
        args.putString(ARG_NOTEID, noteId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            this.username = getArguments().getString(ARG_USERNAME);
            this.collaborationId = getArguments().getString(ARG_COLLABID);
            this.noteId = getArguments().getString(ARG_NOTEID);
        }

        try {
            final Collaboration collaboration = LocalStorageUtils.readCollaborationFromFile(getContext(), collaborationId);
            note = collaboration.getNote(noteId);
            if (note.getLocation() != null) {
                this.mapManager = new MapManager(note.getLocation(), getContext());
            } else {
                this.mapManager = new MapManager(MapManager.NO_LOCATION, getContext());
            }
            this.mapManager.addObserver(this);
        } catch (final IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.editdone_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();

        if (id == R.id.action_editdone) {
            checkUserNoteUpdate();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_edit_note, container, false);
        initializeGuiComponent(rootView);

        return rootView;
    }

    private void initializeGuiComponent(View rootView){
        txtContentNoteEdited = rootView.findViewById(R.id.txtNoteContentEdit);
        txtContentNoteEdited.setText(note.getContent());
        txtContentNoteEdited.requestFocus();
        SupportPlaceAutocompleteFragment autocompleteFragmentEdited = new SupportPlaceAutocompleteFragment();
        getFragmentManager().beginTransaction().replace(R.id.place_autocomplete_fragment_edit, autocompleteFragmentEdited).commit();
        autocompleteFragmentEdited.setOnPlaceSelectedListener(this.mapManager);

        spinnerEditState = rootView.findViewById(R.id.spinnerEditNoteState);
        setSpinner();

        dateViewEdited = rootView.findViewById(R.id.txtEditDateSelected);
        timeViewEdited = rootView.findViewById(R.id.txtEditTimeSelected);
        ImageButton btnSetDateExpiration = rootView.findViewById(R.id.btnEditDateExpiration);
        ImageButton btnSetTimeExpiration = rootView.findViewById(R.id.btnEditTimeExpiration);
        if (note.getExpirationDate() != null) {
            dateViewEdited.setText(note.getExpirationDate().toLocalDate().toString());
            timeViewEdited.setText(note.getExpirationDate().toLocalTime().toString());
            btnSetDateExpiration.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new DatePickerDialog(getActivity(), myDateListenerEdited,
                            note.getExpirationDate().getYear(),
                            note.getExpirationDate().getMonthOfYear(),
                            note.getExpirationDate().getDayOfMonth()).show();
                }
            });
            btnSetTimeExpiration.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new TimePickerDialog(getActivity(), myTimeListenerEdited,
                            note.getExpirationDate().getHourOfDay(),
                            note.getExpirationDate().getMinuteOfHour(), true).show();
                }
            });
        } else {
            final Calendar calendar = Calendar.getInstance();
            btnSetDateExpiration.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new DatePickerDialog(getActivity(), myDateListenerEdited,
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)).show();
                }
            });
            btnSetTimeExpiration.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new TimePickerDialog(getActivity(), myTimeListenerEdited,
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE), true).show();
                }
            });
        }

        myDateListenerEdited = this;
        myTimeListenerEdited = this;
    }

    private void setSpinner(){
        List<NoteProjectState> stateList = new ArrayList<>();
        stateList.addAll(Arrays.asList(NoteProjectState.values()));
        ArrayAdapter<NoteProjectState> dataAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, stateList);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEditState.setAdapter(dataAdapter);
        spinnerEditState.setOnItemSelectedListener(this);
    }

    private void checkUserNoteUpdate(){
        String insertedNoteName = txtContentNoteEdited.getText().toString();
        if(insertedNoteName.equals("")){
            txtContentNoteEdited.setError(getResources().getString(R.string.fieldempty));
        }else{
            note.modifyContent(insertedNoteName);
            if (isDateTimeValid()) {
                note.modifyExpirationDate(new DateTime(year, month, day, hour, minute));
            }
            note.modifyState(new NoteState(noteStateEdited, null));

            new SendMessageToServerTask(getContext()).execute(new ConcreteNoteUpdateMessage(
                username, note, UpdateMessageType.UPDATING, collaborationId));

            ((MainActivity)getActivity()).showLoadingSpinner();
            new TimeoutSender(getContext(), 5000);
        }
    }

    private boolean isDateTimeValid() {
        return year > 0 && month > 0 && day > 0 && hour >=0 && minute >= 0;
    }


    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        this.mapManager.createMap((MapView) view.findViewById(R.id.mapViewLocationEdit), savedInstanceState);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        NoteProjectState item = (NoteProjectState) adapterView.getItemAtPosition(i);
        noteStateEdited = item.toString();
        Log.println(Log.ERROR, "ERRORONI", ""+noteStateEdited);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        Toast toast = Toast.makeText(getActivity().getApplicationContext(), ERR_STATENOTSELECTED, Toast.LENGTH_LONG);
        toast.show();
    }

    @Override
    public void onDateSet(DatePicker datePicker, int year, int month, int day) {
        this.year = year;
        this.month = month + 1;
        this.day = day;
        showDate();
    }

    @Override
    public void onTimeSet(TimePicker timePicker, int hour, int minute) {
        this.hour = hour;
        this.minute = minute;
        showTime();
    }

    private void showDate() {
        dateViewEdited.setText(new StringBuilder().append(day).append("/")
                .append(month).append("/").append(year));
    }
    private void showTime() {
        timeViewEdited.setText(new StringBuilder().append(hour).append(":").append(minute));
    }

    @Override
    public void notify(final Location location) {
        this.note.modifyLocation(location);
    }
}
