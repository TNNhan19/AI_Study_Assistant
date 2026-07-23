package com.example.aistudyassistant.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.models.Schedule;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder> {

    private final Context context;
    private final List<Schedule> schedules;
    private final boolean showDeleteButton;
    private OnScheduleClickListener listener;

    public interface OnScheduleClickListener {
        void onScheduleClick(Schedule schedule);
        void onScheduleDelete(Schedule schedule, int position);
        void onScheduleCompleted(Schedule schedule, int position, boolean completed);
    }

    public ScheduleAdapter(Context context, List<Schedule> schedules) {
        this(context, schedules, true);
    }

    public ScheduleAdapter(Context context, List<Schedule> schedules, boolean showDeleteButton) {
        this.context = context;
        this.schedules = schedules;
        this.showDeleteButton = showDeleteButton;
    }

    public void setListener(OnScheduleClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_schedule, parent, false);
        return new ScheduleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
        holder.bind(schedules.get(position), position);
    }

    @Override
    public int getItemCount() {
        return schedules.size();
    }

    public void removeAt(int position) {
        schedules.remove(position);
        notifyItemRemoved(position);
    }

    public void updateSchedules(List<Schedule> newSchedules) {
        if (schedules == newSchedules) {
            notifyDataSetChanged();
            return;
        }
        schedules.clear();
        schedules.addAll(newSchedules);
        notifyDataSetChanged();
    }

    class ScheduleViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvAmPm, tvTitle, tvDescription, tvDate, tvStatus;
        ImageButton btnDelete, btnComplete;

        ScheduleViewHolder(View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvAmPm = itemView.findViewById(R.id.tv_ampm);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvDescription = itemView.findViewById(R.id.tv_description);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvStatus = itemView.findViewById(R.id.tv_status);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            btnComplete = itemView.findViewById(R.id.btn_complete);
        }

        void bind(Schedule schedule, int position) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(schedule.getDateTimeMillis());

            tvTime.setText(new SimpleDateFormat("h:mm", Locale.getDefault())
                    .format(new Date(schedule.getDateTimeMillis())));
            tvAmPm.setText(new SimpleDateFormat("a", Locale.getDefault())
                    .format(new Date(schedule.getDateTimeMillis())));

            tvTitle.setText(schedule.getTitle());
            tvDescription.setText(schedule.getDescription() != null
                    && !schedule.getDescription().isEmpty()
                    ? schedule.getDescription() : "No description");
            tvDate.setText(new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    .format(new Date(schedule.getDateTimeMillis())));
            tvStatus.setText(schedule.isCompleted()
                    ? context.getString(R.string.schedule_completed)
                    : context.getString(R.string.schedule_pending));
            btnComplete.setContentDescription(schedule.isCompleted()
                    ? context.getString(R.string.mark_pending)
                    : context.getString(R.string.mark_completed));
            btnDelete.setVisibility(showDeleteButton ? View.VISIBLE : View.GONE);
            btnComplete.setVisibility(showDeleteButton ? View.VISIBLE : View.GONE);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onScheduleClick(schedule);
            });
            btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onScheduleDelete(schedule, position);
            });
            btnComplete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onScheduleCompleted(schedule, position, !schedule.isCompleted());
                }
            });
        }
    }
}
