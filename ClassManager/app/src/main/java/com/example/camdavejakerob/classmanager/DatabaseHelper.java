package com.example.camdavejakerob.classmanager;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;

import static android.support.v4.content.ContextCompat.startForegroundService;

/**
 * Created by Rob on 3/13/2018.
 */

public class DatabaseHelper {

    //Strings used frequently for accessing data in Firebase
    private String CIDS = "cids", UIDS = "uids";
    private String ROSTER = "roster", DAYS = "daysOfClass", TIME_END = "endTime", TIME_START = "startTime";
    private String SYLLABUS = "syllabus", CLASS_NAME = "name", ROOM = "room";
    private String CLASSES = "classes", USER_NAME = "name", INSTRUCTOR = "instructor";
    private String ASSIGNMENTS = "assignments", DUE_DATE = "dueDate", GRADES = "grades", SUBMISSIONS = "submissions";
    private String TAG = "DATABASE_HELPER";

    private FirebaseDatabase mDatabase;
    private FirebaseStorage mStorage;


    DatabaseHelper(){
        mDatabase = FirebaseDatabase.getInstance();
        mStorage = FirebaseStorage.getInstance();
    }


    /*****************************************************************************************************************
     *
     *                                  Accessors
     *
     ****************************************************************************************************************/



    public void getCurrentUser(final Context context){
        String userId = FirebaseAuth.getInstance().getUid();
        if(userId == null){ return; }

        mDatabase.getReference(UIDS).child(userId).addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String uid, name;
                Boolean instructor;

                uid = FirebaseAuth.getInstance().getUid();
                name = dataSnapshot.child(USER_NAME).getValue().toString();
                instructor = (Boolean) dataSnapshot.child(INSTRUCTOR).getValue();

                User user = new User(uid,name,instructor);
                ((ClassManagerApp) context.getApplicationContext()).setCurUser(user);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "onCancelled: " + databaseError.toString());
            }
        });
    }

    public void getAllAssignmentSubmissions(final String cid, final String assignmentName, final ListView listView, final Context context){
        mDatabase.getReference().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                GradingHelperAdapter gradeAdapter;
                ArrayList<GradingHelper> submissions = new ArrayList<GradingHelper>();

                for(DataSnapshot rosterSnapshot: dataSnapshot.child(CIDS)
                        .child(cid).child(ROSTER).getChildren()){

                    // the instructor is given false when the course is created
                    // make sure we do not display the instructor in the list
                    if((Boolean) rosterSnapshot.getValue()) {
                        //get the userId
                        String grade, path;
                        Boolean submitted;

                        String uid = rosterSnapshot.getKey();               // get user id
                        String name = dataSnapshot.child(UIDS).child(uid)
                                .child(USER_NAME).getValue().toString();    // get users name


                        DataSnapshot assignmentSnapshot = dataSnapshot.child(CIDS)
                                .child(cid).child(ASSIGNMENTS).child(assignmentName);

                        //see if they have submitted for the chosen homework
                        if(assignmentSnapshot.child(SUBMISSIONS).child(uid).exists()) {
                            //IF YES
                            // set the text view giving the affirmative
                            submitted = true;
                            // set the invisible textview with the download path
                            path = assignmentSnapshot.child(SUBMISSIONS).child(uid).getValue().toString();

                            // check for grade
                            if(assignmentSnapshot.child(GRADES).child(uid).exists()) {
                                //IF YES
                                // show grade
                                grade = assignmentSnapshot.child(GRADES).child(uid).getValue().toString();
                            } else {
                                //IF NO
                                // say its not graded yet
                                grade = "Not yet graded.";
                            }
                        } else {
                            //IF NO
                            // set the text view giving the negative
                            submitted = false;
                            // set the invisible textview with an empty string
                            path = "";

                            //see if they have submitted for the chosen assignment
                            if(assignmentSnapshot.child(GRADES).child(uid).exists()) {
                                //IF YES
                                // show grade
                                grade = assignmentSnapshot.child(GRADES).child(uid).getValue().toString();
                            } else {
                                //IF NO
                                // say its not graded yet
                                grade = "Not yet graded.";
                            }
                        }

                        submissions.add(new GradingHelper(name,uid,path,grade,submitted));

                    }

                }

                gradeAdapter = new GradingHelperAdapter(context,submissions);
                listView.setAdapter(gradeAdapter);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG,"onCancelled: getAllAssignmentSubmissions " + databaseError.toString());
            }
        });
    }

    public void getEnrolledStudents(final Context context, final ListView listView, final String cid){

        mDatabase.getReference().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                RosterAdapter rosterAdapter;
                final ArrayList<User> users = new ArrayList<User>();

                for(DataSnapshot rosterData: dataSnapshot.child(CIDS)
                        .child(cid).child(ROSTER).getChildren()){

                    if((Boolean) rosterData.getValue()) {
                        String name, uid;

                        uid = rosterData.getKey().toString();
                        name = dataSnapshot.child(UIDS).child(uid).child(USER_NAME).getValue().toString();

                        users.add(new User(uid, name, false));
                    }
                }
                rosterAdapter = new RosterAdapter(context,users);
                listView.setAdapter(rosterAdapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "onCancelled: " + databaseError.toString());
            }
        });
    }

    /**
     *
     * @param context
     * @param listView
     * @param uid
     * @param cid
     */
    public void getUserGrades(final Context context, final ListView listView, final String uid, final String cid){
        mDatabase.getReference(CIDS).child(cid).child(ASSIGNMENTS)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        GradeAdapter gradeAdapter;
                        final ArrayList<Assignment> assignments = new ArrayList<Assignment>();

                        for(DataSnapshot assignmentSnapshot: dataSnapshot.getChildren()){

                            String name,grade,dueDate;

                            name = assignmentSnapshot.getKey().toString();
                            dueDate = assignmentSnapshot.child(DUE_DATE).getValue().toString();
                            if(assignmentSnapshot.child(GRADES).child(uid).getValue() == null){
                                grade = "This has not been graded yet.";
                            } else {
                                grade = assignmentSnapshot.child(GRADES).child(uid).getValue().toString();
                            }

                            assignments.add(new Assignment(dueDate,grade,name));
                        }
                        gradeAdapter = new GradeAdapter(context,assignments);
                        listView.setAdapter(gradeAdapter);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d(TAG, "onCancelled: " + databaseError.toString());
                    }
                });
    }

    /**
     *
     * @param context
     * @param listView
     * @param uid
     * @param cid
     */
    public void getUserAssignment(final Context context, final ListView listView, final String uid, final String cid){
        mDatabase.getReference(CIDS).child(cid).child(ASSIGNMENTS)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        AssignmentAdapter assignmentAdapter;
                        final ArrayList<Assignment> assignments = new ArrayList<Assignment>();

                        for(DataSnapshot assignmentSnapshot: dataSnapshot.getChildren()){

                            String name,grade,dueDate;

                            name = assignmentSnapshot.getKey().toString();
                            dueDate = assignmentSnapshot.child(DUE_DATE).getValue().toString();
                            if(assignmentSnapshot.child(GRADES).child(uid).getValue() == null){
                                grade = "not yet graded";
                            } else {
                                grade = assignmentSnapshot.child(GRADES).child(uid).getValue().toString();
                            }

                            assignments.add(new Assignment(dueDate,grade,name));
                        }
                        assignmentAdapter = new AssignmentAdapter(context,assignments);
                        listView.setAdapter(assignmentAdapter);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d(TAG, "onCancelled: " + databaseError.toString());
                    }
                });
    }
    /**
     * updates a list view with all available classes in the database
     *
     * @param context
     * @param listView
     */
    public void updateListViewListOfClasses(final Context context, final ListView listView){
        mDatabase.getReference(CIDS).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                ClassAdapter classAdapter;
                final ArrayList<Class> classes = new ArrayList<Class>();
                ArrayList<String> days = new ArrayList<String>();

                for(DataSnapshot classSnapshot: dataSnapshot.getChildren()){

                    String name,startTime,endTime,room,cid;
                    cid = classSnapshot.getKey().toString();
                    name=classSnapshot.child(CLASS_NAME).getValue().toString();
                    startTime=classSnapshot.child(TIME_START).getValue().toString();
                    endTime=classSnapshot.child(TIME_END).getValue().toString();
                    room=classSnapshot.child(ROOM).getValue().toString();

                    classes.add(new Class(name,days,startTime,endTime,room,cid));
                }
                classAdapter = new ClassAdapter(context,classes);
                listView.setAdapter(classAdapter);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "onCancelled: " + databaseError.toString());
            }
        });
    }

    /**
     *
     * @param context
     * @param listView
     * @param uid
     */
    public void updateListViewUserClasses(final Context context, final ListView listView, final String uid){
        mDatabase.getReference().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                ClassAdapter classAdapter;
                final ArrayList<Class> classes = new ArrayList<Class>();
                ArrayList<String> days = new ArrayList<String>();

                for(DataSnapshot classSnapshot: dataSnapshot.child(UIDS).child(uid).child(CLASSES).getChildren()){

                    String cid = classSnapshot.getKey().toString();

                    DataSnapshot classData = dataSnapshot.child(CIDS).child(cid);

                    Log.d("DatabaseHelper.Java","before classdata class name");
                    String name,startTime,endTime,room;
                    name=classData.child(CLASS_NAME).getValue().toString();
                    startTime=classData.child(TIME_START).getValue().toString();
                    endTime=classData.child(TIME_END).getValue().toString();
                    room=classData.child(ROOM).getValue().toString();
                    classes.add(new Class(name,days,startTime,endTime,room,cid));
                }
                Log.d("DatabaseHelper.Java","after every class is snapshottede");
                classAdapter = new ClassAdapter(context,classes);
                listView.setAdapter(classAdapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "onCancelled: " + databaseError.toString());
            }
        });
    }

    /*****************************************************************************************************************
     *
     *                                  Mutaters
     *
     ****************************************************************************************************************/

    /**
     * writes a class to the database with its name and due date
     *
     * @param cid a string representation of the cass id. This method assumes its a valid class id
     * @param assignment an assignment class
     */
    public void writeAssignment(String cid, Assignment assignment){
        DatabaseReference classRef = mDatabase.getReference(CIDS).child(cid);
        classRef.child(ASSIGNMENTS).child(assignment.getName()).child(DUE_DATE).setValue(assignment.getDueDate());
    }

    /**
     * Adds a grade for a specified student in the given class on a given assignment
     *
     * @param cid String of the class id, assumed to be valid
     * @param uid String of the students id, assumed to be valid
     * @param grade String of the grade which the student received for the assignment
     * @param assignmentName String of the assignments name
     */
    public void writeAssignmentGrade(String cid, String uid, String grade, String assignmentName){
        DatabaseReference classRef = mDatabase.getReference(CIDS).child(cid);
        classRef.child(ASSIGNMENTS).child(assignmentName).child(GRADES).child(uid).setValue(grade);
    }

    /**
     * @param cid a string of the class number
     * @param uid a string of the user id
     *
     *  This method adds the user id to the roster list and the class id to the classes list of the user
     *
     */
    public void enrollUserToClass(final String cid, final String uid){
        DatabaseReference classRef = mDatabase.getReference(CIDS).child(cid);
        DatabaseReference userRef = mDatabase.getReference(UIDS).child(uid);
        classRef.child(ROSTER).child(uid).setValue(true);
        userRef.child(CLASSES).child(cid).setValue(true);
    }

    /**
     * This method assumes all data was validated before its called
     *
     * @param name: a string of the classes title
     * @param daysOfClass: an array containing a list of strings for each day this class takes place
     * @param startTime: a string of the class start time. 12 hour format
     * @param endTime: a string of the class end time. 12 hour format
     * @param room: a string of which room the class takes place
     *
     * This method creates a new class object and then adds it to the Firebase database
     */
    public void writeNewClass( final String classId, final String name, final ArrayList<String> daysOfClass,
                               final String startTime, final String endTime, final String room ){

        String curUserId = FirebaseAuth.getInstance().getUid();

        mDatabase.getReference(CIDS).child(classId).child(CLASS_NAME).setValue(name);
        mDatabase.getReference(CIDS).child(classId).child(DAYS).setValue(daysOfClass);
        mDatabase.getReference(CIDS).child(classId).child(TIME_START).setValue(startTime);
        mDatabase.getReference(CIDS).child(classId).child(TIME_END).setValue(endTime);
        mDatabase.getReference(CIDS).child(classId).child(ROOM).setValue(room);
        mDatabase.getReference(CIDS).child(classId).child(ROSTER).child(curUserId).setValue(false);
        mDatabase.getReference(UIDS).child(curUserId).child(CLASSES).child(classId).setValue(true);
    }

    /**
     * This method assumes all data was validated before its called
     *
     * @param name: is a string of the users first and last name
     * @param userId: is a string generated by FirebaseAuth
     *
     * This method creates a new user object and then adds it to the Firebase database with a new uid(user id)
     *   then increments the uid in the database for future users.
     */
    public void writeNewUser( final String name, final String userId ) {

        mDatabase.getReference(UIDS).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if(dataSnapshot.child(userId).getValue() == null) {
                    mDatabase.getReference(UIDS).child(userId).child(USER_NAME).setValue(name);

                    //a method after this one prompts user and updates the instructor value
                    mDatabase.getReference(UIDS).child(userId).child(INSTRUCTOR).setValue(false);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "onCancelled: " + databaseError.toString());
            }
        });
    }

    public void updateUserToInstructor(String uid, boolean bool){
        mDatabase.getReference(UIDS).child(uid).child(INSTRUCTOR).setValue(bool);
    }

    public boolean userIsInstructor(String uid){
        if (
                mDatabase.getReference(UIDS).child(uid).child(INSTRUCTOR).equals(true)
                ) {
            return true;
        }
        else return false;
    }

    public void writeAssignmentSubmission(String uid, String cid, String assignmentName, String downloadUrl){
        mDatabase.getReference(CIDS).child(cid).child(ASSIGNMENTS).child(assignmentName)
                .child(SUBMISSIONS).child(uid).setValue(downloadUrl);
    }
}
