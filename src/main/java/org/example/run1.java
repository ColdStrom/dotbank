package org.example;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;


public class run1 {

    static class Event implements Comparable<Event>{
        int change;
        LocalDate date;

        public Event(int newChange, LocalDate newDate){
            change = newChange;
            date = newDate;
        }

        @Override
        public int compareTo(Event otherDate){
            if(!date.equals(otherDate.date)){
                return date.compareTo(otherDate.date);
            }
            return Integer.compare(change, otherDate.change);
        }
    }

    public static boolean checkCapacity(int maxCapacity, List<Map<String, String>> guests) {

        List<Event> events = new ArrayList<Event>();

        for(Map<String, String> guest : guests){
            LocalDate checkIn = LocalDate.parse(guest.get("check-in"));
            LocalDate checkOut = LocalDate.parse(guest.get("check-out"));

            events.add(new Event(1, checkIn));
            events.add(new Event(-1, checkOut));
        }

        Collections.sort(events);

        int currentGuests = 0;
        for (Event event : events ){
            currentGuests += event.change;
            if (currentGuests > maxCapacity){
                return false;
            }
        }

        return true; // или false
    }


    // Вспомогательный метод для парсинга JSON строки в Map
    private static Map<String, String> parseJsonToMap(String json) {
        Map<String, String> map = new HashMap<>();
        // Удаляем фигурные скобки
        json = json.substring(1, json.length() - 1);


        // Разбиваем на пары ключ-значение
        String[] pairs = json.split(",");
        for (String pair : pairs) {
            String[] keyValue = pair.split(":", 2);
            String key = keyValue[0].trim().replace("\"", "");
            String value = keyValue[1].trim().replace("\"", "");
            map.put(key, value);
        }
        return map;
    }


    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);


        // Первая строка - вместимость гостиницы
        int maxCapacity = Integer.parseInt(scanner.nextLine());


        // Вторая строка - количество записей о гостях
        int n = Integer.parseInt(scanner.nextLine());


        List<Map<String, String>> guests = new ArrayList<>();


        // Читаем n строк, json-данные о посещении
        for (int i = 0; i < n; i++) {
            String jsonGuest = scanner.nextLine();
            // Простой парсер JSON строки в Map
            Map<String, String> guest = parseJsonToMap(jsonGuest);
            guests.add(guest);
        }


        // Вызов функции
        boolean result = checkCapacity(maxCapacity, guests);


        // Вывод результата
        System.out.println(result ? "True" : "False");


        scanner.close();
    }
}