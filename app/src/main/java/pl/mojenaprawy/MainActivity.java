package pl.mojenaprawy;

import android.app.*;
import android.os.Bundle;
import android.database.Cursor;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import android.text.InputType;
import java.time.*;
import java.time.format.*;
import java.util.*;

public class MainActivity extends Activity {
    Db db; LinearLayout root,content; YearMonth month=YearMonth.now(); LocalDate selected=LocalDate.now();
    int navy=Color.rgb(23,45,76), blue=Color.rgb(22,102,187);
    String day(LocalDate d){return d.format(DateTimeFormatter.ISO_LOCAL_DATE);}
    String month(){return month.toString();}
    String money(double n){return String.format(new Locale("pl","PL"),"%.2f zł",n);}
    LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(navy);t.setPadding(10,9,10,9);return t;}
    Button button(String s,Runnable r){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setOnClickListener(v->r.run());return b;}
    EditText field(String hint,String value,int type){EditText e=new EditText(this);e.setHint(hint);e.setText(value);e.setSingleLine(true);e.setInputType(type);return e;}
    void label(LinearLayout l,String s){l.addView(text(s,14));}
    @Override public void onCreate(Bundle b){super.onCreate(b);db=new Db(this);show();}
    void show(){
        root=vertical();root.setBackgroundColor(Color.WHITE);setContentView(root);
        TextView header=text("🔧  Moje Naprawy",25);header.setTextColor(Color.WHITE);header.setBackgroundColor(navy);header.setPadding(20,25,20,25);root.addView(header);
        LinearLayout nav=new LinearLayout(this);root.addView(nav);
        Button prev=button("◀",()->{month=month.minusMonths(1);selected=month.atDay(1);show();});nav.addView(prev,new LinearLayout.LayoutParams(0,-2,1));
        TextView title=text(month.format(DateTimeFormatter.ofPattern("LLLL yyyy",new Locale("pl","PL"))),20);title.setGravity(Gravity.CENTER);nav.addView(title,new LinearLayout.LayoutParams(0,-1,3));
        Button next=button("▶",()->{month=month.plusMonths(1);selected=month.atDay(1);show();});nav.addView(next,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout stats=vertical();stats.setPadding(14,4,14,4);root.addView(stats);
        try(Cursor c=db.totals(month())){if(c.moveToFirst())stats.addView(text("Naprawy: "+c.getInt(0)+"   Razem: "+money(c.getDouble(1))+"\nZapłacone: "+money(c.getDouble(2))+"   Zaległe: "+money(c.getDouble(1)-c.getDouble(2)),15));}
        Set<String> occupied=new HashSet<>();try(Cursor c=db.days(month())){while(c.moveToNext())occupied.add(c.getString(0));}
        String[] weekdays={"Pn","Wt","Śr","Cz","Pt","So","Nd"};LinearLayout weekdaysRow=new LinearLayout(this);root.addView(weekdaysRow);
        for(String wd:weekdays){TextView w=text(wd,12);w.setGravity(Gravity.CENTER);weekdaysRow.addView(w,new LinearLayout.LayoutParams(0,-2,1));}
        int offset=month.atDay(1).getDayOfWeek().getValue()-1;int cells=((offset+month.lengthOfMonth()+6)/7)*7;
        for(int row=0;row<cells/7;row++){LinearLayout week=new LinearLayout(this);root.addView(week);for(int col=0;col<7;col++){
            int n=row*7+col-offset+1;TextView d=text(n>=1&&n<=month.lengthOfMonth()?String.valueOf(n):"",16);d.setGravity(Gravity.CENTER);d.setPadding(0,12,0,12);
            if(n>=1&&n<=month.lengthOfMonth()){LocalDate date=month.atDay(n);if(date.equals(selected)){d.setBackgroundColor(blue);d.setTextColor(Color.WHITE);}else if(occupied.contains(day(date))){d.setBackgroundColor(Color.rgb(218,238,224));}d.setOnClickListener(v->{selected=date;show();});}
            week.addView(d,new LinearLayout.LayoutParams(0,-2,1));}}
        LinearLayout actions=new LinearLayout(this);root.addView(actions);actions.addView(button("+ Naprawa",()->repairForm(-1)),new LinearLayout.LayoutParams(0,-2,1));actions.addView(button("+ Klient",()->clientForm()),new LinearLayout.LayoutParams(0,-2,1));
        ScrollView scroll=new ScrollView(this);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));content=vertical();content.setPadding(12,4,12,12);scroll.addView(content);
        content.addView(text("Naprawy: "+day(selected),19));int count=0;
        try(Cursor c=db.repairs(day(selected))){while(c.moveToNext()){count++;long id=c.getLong(0);String client=c.getString(1),device=c.getString(2),description=c.getString(3),time=c.getString(5);int mins=c.getInt(6);double amount=c.getDouble(7);boolean paid=c.getInt(8)==1;
            LinearLayout card=vertical();card.setPadding(10,8,10,8);card.setBackgroundColor(Color.rgb(244,247,251));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.bottomMargin=10;content.addView(card,cp);
            card.addView(text(device+" — "+client,18));card.addView(text((time.isEmpty()?"":time+"  •  ")+mins+" min  •  "+money(amount)+"\n"+(paid?"✓ Zapłacone":"● Nieopłacone")+(description.isEmpty()?"":"\n"+description),14));
            LinearLayout controls=new LinearLayout(this);card.addView(controls);controls.addView(button("Edytuj",()->repairForm(id)),new LinearLayout.LayoutParams(0,-2,1));controls.addView(button(paid?"Cofnij wpłatę":"Zapłacono",()->{db.setPaid(id,!paid);show();}),new LinearLayout.LayoutParams(0,-2,1));
        }}if(count==0)content.addView(text("Brak napraw w tym dniu.",15));
    }
    void clientForm(){LinearLayout form=vertical();form.setPadding(24,10,24,10);EditText name=field("Imię i nazwisko / firma","",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);EditText phone=field("Telefon (opcjonalnie)","",InputType.TYPE_CLASS_PHONE);form.addView(name);form.addView(phone);
        new AlertDialog.Builder(this).setTitle("Nowy klient").setView(form).setNegativeButton("Anuluj",null).setPositiveButton("Zapisz",(d,w)->{if(name.getText().toString().trim().isEmpty()){Toast.makeText(this,"Wpisz nazwę klienta",Toast.LENGTH_SHORT).show();return;}db.addClient(name.getText().toString().trim(),phone.getText().toString().trim());show();}).show();}
    void repairForm(long id){ArrayList<Long> ids=new ArrayList<>();ArrayList<String> names=new ArrayList<>();try(Cursor c=db.clients()){while(c.moveToNext()){ids.add(c.getLong(0));names.add(c.getString(1));}}if(ids.isEmpty()){new AlertDialog.Builder(this).setMessage("Najpierw dodaj klienta.").setPositiveButton("Dodaj klienta",(d,w)->clientForm()).setNegativeButton("Anuluj",null).show();return;}
        long clientId=ids.get(0);String device="",description="",date=day(selected),time="09:00",minutes="60",amount="0";boolean paid=false;
        if(id>=0){try(Cursor c=db.repair(id)){if(c.moveToFirst()){clientId=c.getLong(0);device=c.getString(1);description=c.getString(2);date=c.getString(3);time=c.getString(4);minutes=String.valueOf(c.getInt(5));amount=String.valueOf(c.getDouble(6));paid=c.getInt(7)==1;}}}
        ScrollView scroll=new ScrollView(this);LinearLayout form=vertical();form.setPadding(24,8,24,8);scroll.addView(form);
        label(form,"Klient");Spinner clients=new Spinner(this);ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names);clients.setAdapter(adapter);int pos=ids.indexOf(clientId);clients.setSelection(Math.max(pos,0));form.addView(clients);
        label(form,"Urządzenie / rodzaj naprawy");EditText eDevice=field("Np. naprawa falownika",device,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);form.addView(eDevice);
        label(form,"Opis prac");EditText eDesc=field("Co zostało wykonane",description,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);eDesc.setSingleLine(false);eDesc.setMinLines(2);form.addView(eDesc);
        label(form,"Data (RRRR-MM-DD)");EditText eDate=field("2026-10-31",date,InputType.TYPE_CLASS_DATETIME);form.addView(eDate);
        label(form,"Godzina rozpoczęcia (GG:MM)");EditText eTime=field("09:00",time,InputType.TYPE_CLASS_DATETIME);form.addView(eTime);
        label(form,"Czas pracy w minutach");EditText eMinutes=field("60",minutes,InputType.TYPE_CLASS_NUMBER);form.addView(eMinutes);
        label(form,"Cena w zł (wpisywana ręcznie)");EditText eAmount=field("350",amount,InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);form.addView(eAmount);
        CheckBox check=new CheckBox(this);check.setText("Zapłacone");check.setChecked(paid);form.addView(check);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(id<0?"Nowa naprawa":"Edytuj naprawę").setView(scroll).setNegativeButton("Anuluj",null).setPositiveButton("Zapisz",null).create();
        if(id>=0)dialog.setButton(AlertDialog.BUTTON_NEUTRAL,"Usuń",(d,w)->new AlertDialog.Builder(this).setMessage("Usunąć naprawę?").setNegativeButton("Anuluj",null).setPositiveButton("Usuń",(dd,ww)->{db.deleteRepair(id);show();}).show());
        dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{
            String dev=eDevice.getText().toString().trim(),dt=eDate.getText().toString().trim(),tm=eTime.getText().toString().trim();if(dev.isEmpty())throw new IllegalArgumentException("Wpisz rodzaj naprawy");LocalDate parsed=LocalDate.parse(dt);LocalTime.parse(tm);int min=Integer.parseInt(eMinutes.getText().toString().trim());double val=Double.parseDouble(eAmount.getText().toString().trim().replace(',','.'));if(min<0||val<0||!Double.isFinite(val))throw new IllegalArgumentException("Czas i cena nie mogą być ujemne");
            db.saveRepair(id,ids.get(clients.getSelectedItemPosition()),dev,eDesc.getText().toString(),dt,tm,min,val,check.isChecked());selected=parsed;month=YearMonth.from(parsed);dialog.dismiss();show();
        }catch(Exception ex){Toast.makeText(this,"Sprawdź dane: "+ex.getMessage(),Toast.LENGTH_LONG).show();}}));dialog.show();
    }
}
