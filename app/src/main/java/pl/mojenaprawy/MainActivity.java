package pl.mojenaprawy;

import android.app.*;
import android.os.Bundle;
import android.database.Cursor;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.Editable;
import java.time.*;
import java.time.format.*;
import java.util.*;

public class MainActivity extends Activity {
    Db db; LinearLayout root,content; YearMonth month=YearMonth.now(); LocalDate selected=LocalDate.now();
    boolean clientsScreen=false;
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
        if(clientsScreen){showClients();return;}
        root=vertical();root.setBackgroundColor(Color.WHITE);setContentView(root);
        TextView header=text("🔧  Moje Naprawy",25);header.setTextColor(Color.WHITE);header.setBackgroundColor(navy);header.setPadding(20,25,20,25);root.addView(header);
        LinearLayout nav=new LinearLayout(this);root.addView(nav);
        Button prev=button("◀",()->{month=month.minusMonths(1);selected=month.atDay(1);show();});nav.addView(prev,new LinearLayout.LayoutParams(0,-2,1));
        TextView title=text(month.format(DateTimeFormatter.ofPattern("LLLL yyyy",new Locale("pl","PL"))),20);title.setGravity(Gravity.CENTER);nav.addView(title,new LinearLayout.LayoutParams(0,-1,3));
        Button next=button("▶",()->{month=month.plusMonths(1);selected=month.atDay(1);show();});nav.addView(next,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout stats=vertical();stats.setPadding(14,4,14,4);root.addView(stats);
        try(Cursor c=db.totals(month())){if(c.moveToFirst())stats.addView(text("Naprawy: "+c.getInt(0)+"   Razem: "+money(c.getDouble(1))+"\nZapłacone: "+money(c.getDouble(2))+"   Zaległe: "+money(c.getDouble(1)-c.getDouble(2)),15));}
        try(Cursor c=db.partsMonthTotals(month())){if(c.moveToFirst())stats.addView(text("Części: "+money(c.getDouble(0))+"   Zaległe za części: "+money(c.getDouble(0)-c.getDouble(1)),14));}
        Map<String,Integer> statuses=new HashMap<>();try(Cursor c=db.dayStatuses(month())){while(c.moveToNext()){int count=c.getInt(1),paidCount=c.getInt(2);statuses.put(c.getString(0),paidCount==count?1:paidCount==0?2:3);}}
        String[] weekdays={"Pn","Wt","Śr","Cz","Pt","So","Nd"};LinearLayout weekdaysRow=new LinearLayout(this);root.addView(weekdaysRow);
        for(String wd:weekdays){TextView w=text(wd,12);if(wd.equals("Nd"))w.setTextColor(Color.RED);w.setGravity(Gravity.CENTER);weekdaysRow.addView(w,new LinearLayout.LayoutParams(0,-2,1));}
        int offset=month.atDay(1).getDayOfWeek().getValue()-1;int cells=((offset+month.lengthOfMonth()+6)/7)*7;
        for(int row=0;row<cells/7;row++){LinearLayout week=new LinearLayout(this);root.addView(week);for(int col=0;col<7;col++){
            int n=row*7+col-offset+1;TextView d=text(n>=1&&n<=month.lengthOfMonth()?String.valueOf(n):"",16);d.setGravity(Gravity.CENTER);d.setPadding(0,12,0,12);
            if(n>=1&&n<=month.lengthOfMonth()){LocalDate date=month.atDay(n);if(date.getDayOfWeek()==DayOfWeek.SUNDAY||isPolishHoliday(date))d.setTextColor(Color.rgb(190,25,25));
                Integer status=statuses.get(day(date));if(status!=null){d.setBackgroundColor(status==1?Color.rgb(201,239,210):status==2?Color.rgb(255,205,205):Color.rgb(224,206,251));}
                if(date.equals(selected)){android.graphics.drawable.GradientDrawable outline=new android.graphics.drawable.GradientDrawable();outline.setColor(status==null?Color.rgb(231,240,255):status==1?Color.rgb(201,239,210):status==2?Color.rgb(255,205,205):Color.rgb(224,206,251));outline.setStroke(3,blue);d.setBackground(outline);}
                d.setOnClickListener(v->{selected=date;show();});}
            week.addView(d,new LinearLayout.LayoutParams(0,-2,1));}}
        LinearLayout actions=new LinearLayout(this);root.addView(actions);actions.addView(button("+ Naprawa",()->repairForm(-1)),new LinearLayout.LayoutParams(0,-2,1));actions.addView(button("Klienci",()->{clientsScreen=true;show();}),new LinearLayout.LayoutParams(0,-2,1));
        root.addView(text("Kolory:  🟩 opłacone    🟥 nieopłacone    🟪 mieszane",12));
        ScrollView scroll=new ScrollView(this);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));content=vertical();content.setPadding(12,4,12,12);scroll.addView(content);
        content.addView(text("Naprawy: "+day(selected),19));int count=0;
        try(Cursor c=db.repairs(day(selected))){while(c.moveToNext()){count++;long id=c.getLong(0);String client=c.getString(1),device=c.getString(2),description=c.getString(3),time=c.getString(5);int mins=c.getInt(6);double amount=c.getDouble(7);boolean paid=c.getInt(8)==1;
            LinearLayout card=vertical();card.setPadding(10,8,10,8);card.setBackgroundColor(Color.rgb(244,247,251));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.bottomMargin=10;content.addView(card,cp);
            card.addView(text(device+" — "+client,18));card.addView(text((time.isEmpty()?"":time+"  •  ")+mins+" min  •  "+money(amount)+"\n"+(paid?"✓ Zapłacone":"● Nieopłacone")+(description.isEmpty()?"":"\n"+description),14));
            try(Cursor ps=db.partsSummary(id)){if(ps.moveToFirst())card.addView(text("Części: "+money(ps.getDouble(0))+"  •  Do zapłaty: "+money(ps.getDouble(1)),14));}
            card.addView(button("Lista części",()->showParts(id,device,client)));
            LinearLayout controls=new LinearLayout(this);card.addView(controls);controls.addView(button("Edytuj",()->repairForm(id)),new LinearLayout.LayoutParams(0,-2,1));controls.addView(button(paid?"Cofnij wpłatę":"Zapłacono",()->{db.setPaid(id,!paid);show();}),new LinearLayout.LayoutParams(0,-2,1));
        }}if(count==0)content.addView(text("Brak napraw w tym dniu.",15));
    }
    boolean isPolishHoliday(LocalDate d){
        int y=d.getYear(),m=d.getMonthValue(),n=d.getDayOfMonth();
        if((m==1&&(n==1||n==6))||(m==5&&(n==1||n==3))||(m==8&&n==15)||(m==11&&(n==1||n==11))||(m==12&&(n==25||n==26||(n==24&&y>=2025))))return true;
        int a=y%19,b=y/100,c=y%100,dd=b/4,e=b%4,f=(b+8)/25,g=(b-f+1)/3,h=(19*a+b-dd-g+15)%30,i=c/4,k=c%4,l=(32+2*e+2*i-h-k+700)%7,mm=(a+11*h+22*l)/451;
        int em=(h+l-7*mm+114)/31,ed=(h+l-7*mm+114)%31+1;
        LocalDate easter=LocalDate.of(y,em,ed);
        return d.equals(easter)||d.equals(easter.plusDays(1))||d.equals(easter.plusDays(49))||d.equals(easter.plusDays(60));
    }
    void showClients(){
        root=vertical();root.setBackgroundColor(Color.WHITE);setContentView(root);
        TextView header=text("Klienci",24);header.setTextColor(Color.WHITE);header.setBackgroundColor(navy);root.addView(header);
        LinearLayout bar=new LinearLayout(this);root.addView(bar);
        bar.addView(button("◀ Kalendarz",()->{clientsScreen=false;show();}),new LinearLayout.LayoutParams(0,-2,1));
        bar.addView(button("+ Dodaj klienta",()->clientForm(-1,"","")),new LinearLayout.LayoutParams(0,-2,1));
        EditText search=field("Szukaj klienta lub telefonu","",InputType.TYPE_CLASS_TEXT);root.addView(search);
        ScrollView scroll=new ScrollView(this);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout list=vertical();list.setPadding(12,4,12,12);scroll.addView(list);
        Runnable render=()->{
            list.removeAllViews();String query=search.getText().toString().trim().toLowerCase(new Locale("pl","PL"));int found=0;
            try(Cursor c=db.clients()){while(c.moveToNext()){
                long id=c.getLong(0);String name=c.getString(1),phone=c.getString(2);
                if(!(name+" "+phone).toLowerCase(new Locale("pl","PL")).contains(query))continue;
                found++;LinearLayout card=vertical();card.setPadding(10,8,10,8);card.setBackgroundColor(Color.rgb(244,247,251));
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=10;list.addView(card,lp);
                card.addView(text(name,19));if(!phone.isEmpty())card.addView(text("Tel.: "+phone,14));
                try(Cursor stats=db.clientStats(id)){if(stats.moveToFirst()){double due=stats.getDouble(1);try(Cursor pDue=db.clientPartsDue(id)){if(pDue.moveToFirst())due+=pDue.getDouble(0);}card.addView(text("Naprawy: "+stats.getInt(0)+"   Do zapłaty: "+money(due),14));}}
                card.addView(button("Edytuj klienta",()->clientForm(id,name,phone)));
            }}if(found==0)list.addView(text("Brak klientów spełniających kryteria.",15));
        };render.run();search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){render.run();}public void afterTextChanged(Editable e){}});
    }
    void showParts(long repairId,String device,String client){
        LinearLayout page=vertical();page.setBackgroundColor(Color.WHITE);setContentView(page);
        TextView title=text("Części — "+device,22);title.setBackgroundColor(navy);title.setTextColor(Color.WHITE);page.addView(title);
        page.addView(text("Klient: "+client,15));
        page.addView(button("◀ Wróć do kalendarza",()->show()));
        page.addView(button("+ Dodaj część",()->partForm(-1,repairId,"",0,false,device,client)));
        ScrollView scroll=new ScrollView(this);page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));LinearLayout list=vertical();list.setPadding(12,8,12,8);scroll.addView(list);
        int count=0;
        try(Cursor c=db.parts(repairId)){while(c.moveToNext()){
            count++;long id=c.getLong(0);String name=c.getString(1);double amount=c.getDouble(2);boolean paid=c.getInt(3)==1;
            LinearLayout card=vertical();card.setPadding(8,8,8,8);card.setBackgroundColor(Color.rgb(244,247,251));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=10;list.addView(card,lp);
            card.addView(text(name+" — "+money(amount),18));card.addView(text(paid?"✓ Zapłacona":"● Nieopłacona",15));
            LinearLayout actions=new LinearLayout(this);card.addView(actions);
            actions.addView(button("Edytuj",()->partForm(id,repairId,name,amount,paid,device,client)),new LinearLayout.LayoutParams(0,-2,1));
            actions.addView(button(paid?"Cofnij wpłatę":"Zapłacono",()->{db.setPartPaid(id,!paid);showParts(repairId,device,client);}),new LinearLayout.LayoutParams(0,-2,1));
        }}
        if(count==0)list.addView(text("Nie dodano jeszcze części.",16));
        try(Cursor c=db.partsSummary(repairId)){if(c.moveToFirst())list.addView(text("Suma części: "+money(c.getDouble(0))+"\nDo zapłaty: "+money(c.getDouble(1)),18));}
    }
    void partForm(long id,long repairId,String oldName,double oldAmount,boolean oldPaid,String device,String client){
        LinearLayout form=vertical();form.setPadding(22,12,22,12);
        label(form,"Nazwa części");EditText name=field("Np. łożysko 6205",oldName,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);form.addView(name);
        label(form,"Kwota do zapłaty przez klienta (zł)");EditText amount=field("0,00",id<0?"":String.valueOf(oldAmount),InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);form.addView(amount);
        CheckBox paid=new CheckBox(this);paid.setText("Zapłacona przez klienta");paid.setChecked(oldPaid);form.addView(paid);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(id<0?"Dodaj część":"Edytuj część").setView(form).setNegativeButton("Anuluj",null).setPositiveButton("Zapisz",null).create();
        if(id>=0)dialog.setButton(AlertDialog.BUTTON_NEUTRAL,"Usuń",(d,w)->new AlertDialog.Builder(this).setMessage("Usunąć tę część?").setNegativeButton("Anuluj",null).setPositiveButton("Usuń",(dd,ww)->{db.deletePart(id);showParts(repairId,device,client);}).show());
        dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{
            String n=name.getText().toString().trim();if(n.isEmpty()){name.setError("Wpisz nazwę części");return;}
            double val=Double.parseDouble(amount.getText().toString().trim().replace(',','.'));if(val<0||!Double.isFinite(val))throw new IllegalArgumentException("Nieprawidłowa cena");
            db.savePart(id,repairId,n,val,paid.isChecked());dialog.dismiss();showParts(repairId,device,client);
        }catch(Exception ex){Toast.makeText(this,"Sprawdź kwotę części",Toast.LENGTH_LONG).show();}}));dialog.show();
    }
    void clientForm(long id,String oldName,String oldPhone){
        LinearLayout form=vertical();form.setPadding(24,10,24,10);
        EditText name=field("Imię i nazwisko / firma",oldName,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        EditText phone=field("Telefon (opcjonalnie)",oldPhone,InputType.TYPE_CLASS_PHONE);form.addView(name);form.addView(phone);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(id<0?"Nowy klient":"Edytuj klienta").setView(form).setNegativeButton("Anuluj",null).setPositiveButton("Zapisz",null).create();
        dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String n=name.getText().toString().trim();if(n.isEmpty()){name.setError("Wpisz nazwę klienta");return;}if(id<0)db.addClient(n,phone.getText().toString().trim());else db.updateClient(id,n,phone.getText().toString().trim());dialog.dismiss();show();}));dialog.show();
    }
    void clientForm(){clientForm(-1,"","");}
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
