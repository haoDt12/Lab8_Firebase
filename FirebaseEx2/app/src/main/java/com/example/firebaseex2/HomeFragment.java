package com.example.firebaseex2;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    //AuthStateListener that responds to changes in the user's sign-in state
    private FirebaseAuth.AuthStateListener fAuthListener;
    private FirebaseAuth fAuth;
    private FirebaseUser user;
    //Database references for handling CRUD operations
    private DatabaseReference mDatabase;
    public static HomeFragment homeFragment;
    private ProgressDialog mProgressDialog;
    private TextView msgTxt;

    private ListView listView;
    private CustomListAdapter customListAdapter;
    private static ArrayList<Items> arrayListTodo = new ArrayList<>();
    private ArrayList<String> keysArray;

    public HomeFragment() {
        // Required empty public constructor
    }

    public static Fragment newInstance() {
        homeFragment = new HomeFragment();
        return homeFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);
        setHasOptionsMenu(true);
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_change_email) {
            showEditTextDialog(1); //To change email id
        } else if (item.getItemId() == R.id.action_change_password) {
            showEditTextDialog(0); //To change password
        } else if (item.getItemId() == R.id.action_delete_account) {
            deleteAccountAction(); //To delete user account
        } else if (item.getItemId() == R.id.action_logout) {
            signOutUser();
        }
        return super.onOptionsItemSelected(item);
    }

    private void mFragmentTransaction(Fragment mFragment) {
        FragmentManager fm = getFragmentManager();
        Fragment oldFragment = fm.findFragmentByTag(MainActivity.FRAGMENT_TAG);
        FragmentTransaction ft = fm.beginTransaction();
        if (oldFragment != null) ft.remove(oldFragment);
        ft.replace(R.id.content, mFragment, MainActivity.FRAGMENT_TAG);
        ft.addToBackStack("replace");
        ft.commit();
    }

    private void showProgressDialog(String title, String message) {
        if (mProgressDialog != null && mProgressDialog.isShowing())
            mProgressDialog.setMessage(message);
        else mProgressDialog = ProgressDialog.show(getActivity(), title, message, true, false);
    }

    //Used to hide the progress dialog if displaying
    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        msgTxt = (TextView) getView().findViewById(R.id.textView);
        fAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        user = fAuth.getCurrentUser();
        fAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (user == null) {
                    mFragmentTransaction(LoginSignUpFragment.newInstance());
                } else {
                    String name = user.getDisplayName();
                    String email = user.getEmail();
                    Uri photoUrl = user.getPhotoUrl();

                    String uid = user.getUid();
                    msgTxt.setText("Hello " + email);
                }
            }
        };
        getView().findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showEditTextDialog(2);
            }
        });
        keysArray = new ArrayList<>();
        listView = (ListView) getView().findViewById(R.id.listView);

        customListAdapter = new CustomListAdapter(getActivity(), arrayListTodo, homeFragment);
        listView.setAdapter(customListAdapter);

        TextView headerText = new TextView(getActivity());
        headerText.setText("ToDo Items");
        listView.addHeaderView(headerText);
        listView.setEmptyView(getView().findViewById(R.id.emptyTxt));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                updateTodoItemDetails(arrayListTodo.get(i), i);
            }
        });
    }

    ChildEventListener childEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
            Log.d(TAG, dataSnapshot.getKey() + ":" + dataSnapshot.getValue().toString());
            String addedKey = dataSnapshot.getKey();
            if (!keysArray.contains(addedKey)) {
                Items todoItem = dataSnapshot.getValue(Items.class);
                arrayListTodo.add(todoItem);
                keysArray.add(dataSnapshot.getKey());
                updateView();
            }
        }
        @Override
        public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
            Log.d(TAG, dataSnapshot.getKey() + ":" + dataSnapshot.getValue().toString());
            String changedKey = dataSnapshot.getKey();
            int changedIndex = keysArray.indexOf(changedKey);
            if (changedIndex > -1) { //Checking for key exist or not
                Items todoItem = dataSnapshot.getValue(Items.class);
                arrayListTodo.set(changedIndex, todoItem);
                updateView();
            }
        }

        @Override
        public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
            Log.d(TAG, dataSnapshot.getKey() + ":" +dataSnapshot.getValue().toString());
            String deletedKey = dataSnapshot.getKey();
            int removedIndex = keysArray.indexOf(deletedKey);
            if (removedIndex > -1) { //Checking for key exist or not
                keysArray.remove(removedIndex);
                arrayListTodo.remove(removedIndex);
                updateView();
            }
        }
        @Override
        public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

        }
        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            Toast.makeText(getActivity(), "Could not update.", Toast.LENGTH_SHORT).show();
            updateView();
        }
};

    public void updateView() {
        customListAdapter.notifyDataSetChanged();
        listView.invalidate();
    }

    public void addNewTodoItem(Items model) {
        String key = mDatabase.child("users").child(user.getUid()).push().getKey();
        Map<String, Object> todoValues = model.toMap();
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/users/" + user.getUid() + "/" + key, todoValues);
        mDatabase.updateChildren(childUpdates);
    }

    public void deleteTodoItem(int position) {
        String clickedKey = keysArray.get(position);
        mDatabase.child("users").child(user.getUid()).child(clickedKey).removeValue();
    }

    public void updateTodoItemDetails(Items model, int position) {
        String clickedKey = keysArray.get(position);
        mDatabase.child("users").child(user.getUid()).child(clickedKey).setValue(model);
    }

    private void signOutUser() {
        fAuth.signOut();
        mFragmentTransaction(LoginSignUpFragment.newInstance());
    }

    private void deleteAccountAction() {
        showProgressDialog("Delete Account", "Loading..");
        if (user != null) {
//You can delete a user account with the delete method.
            user.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override

                public void onComplete(@NonNull Task<Void> task) {

                    if (task.isSuccessful()) {
                        Toast.makeText(getActivity(), "Your profile is deleted:(Create a account now !", Toast.LENGTH_SHORT).show();
                        mFragmentTransaction(LoginSignUpFragment.newInstance());
                        hideProgressDialog();
                    } else {
                        Toast.makeText(getActivity(), "Failed to delete your account !", Toast.LENGTH_SHORT).show();
                        hideProgressDialog();
                    }
                }
            });
        }
    }

    private void changePasswordAction(String oldEmailId) {
        showProgressDialog("Change Account Password", "Loading..");
        if (user != null) {
            fAuth.sendPasswordResetEmail(oldEmailId).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override

                public void onComplete(@NonNull Task<Void> task) {

                    if (task.isSuccessful()) {
                        Toast.makeText(getActivity(), "Reset password email is sent!", Toast.LENGTH_SHORT).show();
                        hideProgressDialog();
                    } else {
                        Toast.makeText(getActivity(), "Failed to send reset email!", Toast.LENGTH_SHORT).show();
                        hideProgressDialog();
                    }
                }
            });
        } else {
            hideProgressDialog();
        }
    }

    private void changeEmailAction(String newEmailId) {
        showProgressDialog("Change Account Email", "Loading..");
        if (user != null) {
            user.updateEmail(newEmailId).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override

                public void onComplete(@NonNull Task<Void> task) {

                    if (task.isSuccessful()) {
                        Toast.makeText(getActivity(), "Email address is updated.Please sign in with new email id !", Toast.LENGTH_LONG).show();
                        signOutUser(); //signing out the user
                        hideProgressDialog();
                    } else {
                        Toast.makeText(getActivity(), "Failed to update email !", Toast.LENGTH_LONG).show();
                        hideProgressDialog();
                    }
                }
            });
        } else {
            hideProgressDialog();
        }
    }

    public void showEditTextDialog(final int forWhich) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_change_user_credential, null);
        dialogBuilder.setView(dialogView);
        final EditText edt = (EditText) dialogView.findViewById(R.id.edit1);
        if (forWhich == 0) {
            dialogBuilder.setTitle("Changing Password");
            dialogBuilder.setMessage("Enter current email id");
        } else if (forWhich == 1) {
            dialogBuilder.setTitle("Changing Email Id");
            dialogBuilder.setMessage("Enter new email id");
        } else {
            dialogBuilder.setTitle("New ToDo Item");
            dialogBuilder.setMessage("Enter new todo list item");
        }
        dialogBuilder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //do something with edt.getText().toString();

                if (forWhich == 1)

                    changeEmailAction(edt.getText().toString());
                else if (forWhich == 0) changePasswordAction(edt.getText().toString());
                else
                    addNewTodoItem(new Items(user.getUid(), user.getEmail(), edt.getText().toString(), false));
            }
        });
        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
//pass
            }
        });
        AlertDialog b = dialogBuilder.create();
        b.show();
    }
    @Override
    public void onStart() {
        super.onStart();
        fAuth.addAuthStateListener(fAuthListener);
    }
    @Override
    public void onStop() {
        super.onStop();
        if (fAuthListener != null) {
            fAuth.removeAuthStateListener(fAuthListener);
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        if (user != null && mDatabase != null)
            mDatabase.child("users").child(user.getUid()).addChildEventListener(childEventListener);
    }
    @Override
    public void onPause() {
        super.onPause();
        if (mDatabase != null)
            mDatabase.removeEventListener(childEventListener);
    }
}