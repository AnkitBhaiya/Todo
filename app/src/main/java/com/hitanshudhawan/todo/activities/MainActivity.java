package com.hitanshudhawan.todo.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.appwidget.AppWidgetManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TimePicker;

import com.hitanshudhawan.todo.R;
import com.hitanshudhawan.todo.adapters.TodoCursorAdapter;
import com.hitanshudhawan.todo.database.TodoContract;
import com.hitanshudhawan.todo.widget.TodoWidget;

import java.util.Calendar;


public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    RecyclerView mTodoRecyclerView;
    TodoCursorAdapter mTodoCursorAdapter;
    LinearLayout mEmptyView;

    FloatingActionButton mFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initFab();
        initRecyclerView();
    }

    private void initFab() {
        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, TodoAddActivity.class));
            }
        });
    }

    private void initRecyclerView() {
        mTodoRecyclerView = (RecyclerView) findViewById(R.id.todo_recycler_view);
        mTodoCursorAdapter = new TodoCursorAdapter(MainActivity.this);
        mTodoRecyclerView.setAdapter(mTodoCursorAdapter);
        mTodoRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.VERTICAL, false));
        // mTodoRecyclerView.addItemDecoration(new DividerItemDecoration(MainActivity.this,DividerItemDecoration.VERTICAL));
        mEmptyView = (LinearLayout) findViewById(R.id.empty_view);
        getSupportLoaderManager().initLoader(0, null, MainActivity.this);

        mTodoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    mFab.show();
                } else {
                    mFab.hide();
                }
            }
        });

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {

                final long id = viewHolder.getItemId();

                if (direction == ItemTouchHelper.RIGHT) {
                    // Todo Done.
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(TodoContract.TodoEntry.COLUMN_TODO_DONE, TodoContract.TodoEntry.TODO_DONE);
                    getContentResolver().update(ContentUris.withAppendedId(TodoContract.TodoEntry.CONTENT_URI, id), contentValues, null, null);
                    sendBroadcast(new Intent(MainActivity.this, TodoWidget.class).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE));
                    Snackbar doneSnackbar = Snackbar.make(viewHolder.itemView, "Todo Done.", Snackbar.LENGTH_LONG);
                    doneSnackbar.setAction("Undo", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ContentValues contentValues = new ContentValues();
                            contentValues.put(TodoContract.TodoEntry.COLUMN_TODO_DONE, TodoContract.TodoEntry.TODO_NOT_DONE);
                            getContentResolver().update(ContentUris.withAppendedId(TodoContract.TodoEntry.CONTENT_URI, id), contentValues, null, null);
                            sendBroadcast(new Intent(MainActivity.this, TodoWidget.class).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE));
                        }
                    });
                    doneSnackbar.show();
                } else {
                    // Change Date and Time.
                    final Calendar currentDateTime = Calendar.getInstance();
                    final Calendar todoDateTime = Calendar.getInstance();
                    TimePickerDialog timePickerDialog = new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker timePicker, int hourOfDay, int minute) {
                            todoDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                            todoDateTime.set(Calendar.MINUTE, minute);
                            int year, month, dayOfMonth;
                            if (todoDateTime.get(Calendar.HOUR_OF_DAY) * 60 + todoDateTime.get(Calendar.MINUTE) < currentDateTime.get(Calendar.HOUR_OF_DAY) * 60 + currentDateTime.get(Calendar.MINUTE)) {
                                currentDateTime.add(Calendar.DATE, 1);
                                year = currentDateTime.get(Calendar.YEAR);
                                month = currentDateTime.get(Calendar.MONTH);
                                dayOfMonth = currentDateTime.get(Calendar.DAY_OF_MONTH);
                                currentDateTime.add(Calendar.DATE, -1);
                            } else {
                                year = currentDateTime.get(Calendar.YEAR);
                                month = currentDateTime.get(Calendar.MONTH);
                                dayOfMonth = currentDateTime.get(Calendar.DAY_OF_MONTH);
                            }
                            DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                                @Override
                                public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {
                                    todoDateTime.set(Calendar.YEAR, year);
                                    todoDateTime.set(Calendar.MONTH, month);
                                    todoDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                                    ContentValues contentValues = new ContentValues();
                                    contentValues.put(TodoContract.TodoEntry.COLUMN_TODO_DATE_TIME, todoDateTime.getTimeInMillis());
                                    getContentResolver().update(ContentUris.withAppendedId(TodoContract.TodoEntry.CONTENT_URI, id), contentValues, null, null);
                                    sendBroadcast(new Intent(MainActivity.this, TodoWidget.class).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE));
                                }
                            }, year, month, dayOfMonth);
                            Calendar minDateTime = Calendar.getInstance();
                            minDateTime.set(year, month, dayOfMonth);
                            datePickerDialog.getDatePicker().setMinDate(minDateTime.getTimeInMillis());
                            datePickerDialog.show();
                        }
                    }, currentDateTime.get(Calendar.HOUR_OF_DAY), currentDateTime.get(Calendar.MINUTE), DateFormat.is24HourFormat(MainActivity.this));
                    timePickerDialog.show();
                    mTodoCursorAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
                }
            }

            @Override
            public void onChildDraw(Canvas canvas, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                View itemView = viewHolder.itemView;
                float height = (float) itemView.getBottom() - (float) itemView.getTop();
                float imageWidth = ((BitmapDrawable) getResources().getDrawable(R.mipmap.ic_done_white_24dp)).getBitmap().getWidth();

                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    if (dX > 0) {
                        Paint p = new Paint();
                        p.setColor(ContextCompat.getColor(MainActivity.this, R.color.colorTodoDone));
                        RectF background = new RectF((float) itemView.getLeft(), (float) itemView.getTop(), dX, (float) itemView.getBottom());
                        canvas.drawRect(background, p);
                        canvas.clipRect(background);
                        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_done_white_24dp);
                        RectF icon_dest = new RectF((float) itemView.getLeft() + imageWidth, (float) itemView.getTop() + ((height / 2) - (imageWidth / 2)), (float) itemView.getLeft() + 2 * imageWidth, (float) itemView.getBottom() - ((height / 2) - (imageWidth / 2)));
                        canvas.drawBitmap(bitmap, null, icon_dest, p);
                        canvas.restore();
                    } else {
                        Paint p = new Paint();
                        p.setColor(ContextCompat.getColor(MainActivity.this, R.color.colorTodoDateTimeChange));
                        RectF background = new RectF((float) itemView.getRight() + dX, (float) itemView.getTop(), (float) itemView.getRight(), (float) itemView.getBottom());
                        canvas.drawRect(background, p);
                        canvas.clipRect(background);
                        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_date_range_white_24dp);
                        RectF icon_dest = new RectF((float) itemView.getRight() - 2 * imageWidth, (float) itemView.getTop() + ((height / 2) - (imageWidth / 2)), (float) itemView.getRight() - imageWidth, (float) itemView.getBottom() - ((height / 2) - (imageWidth / 2)));
                        canvas.drawBitmap(bitmap, null, icon_dest, p);
                        canvas.restore();
                    }
                }
            }
        }).attachToRecyclerView(mTodoRecyclerView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        SearchView searchView = (SearchView) menu.findItem(R.id.search_item_main).getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // What to do ? for Search.
                return true;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.about_item_main:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        String[] projection = {
                TodoContract.TodoEntry._ID,
                TodoContract.TodoEntry.COLUMN_TODO_TITLE,
                TodoContract.TodoEntry.COLUMN_TODO_DATE_TIME,
                TodoContract.TodoEntry.COLUMN_TODO_DONE};
        return new CursorLoader(MainActivity.this,
                TodoContract.TodoEntry.CONTENT_URI,
                projection,
                TodoContract.TodoEntry.COLUMN_TODO_DONE + " = " + TodoContract.TodoEntry.TODO_NOT_DONE,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mTodoCursorAdapter.swapCursor(data);
        mEmptyView.setVisibility(mTodoCursorAdapter.getItemCount() == 0 ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mTodoCursorAdapter.swapCursor(null);
    }
}