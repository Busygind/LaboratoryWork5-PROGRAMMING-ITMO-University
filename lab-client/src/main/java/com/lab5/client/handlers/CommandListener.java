package com.lab5.client.handlers;

import com.lab5.client.entities.CollectionOfDragons;
import com.lab5.client.entities.Dragon;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Scanner;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;


/**
 * Класс, содержащий методы, вызываемые напрямую после соответствующих команд пользователя,
 * а также методы по обработке полученных данных
 *
 * @author Dmitry Busygin
 */
public class CommandListener {

    /**
     * Словарь, сопоставляющий доступные команды с соответствующими методами
     */
    private static Map<String, Method> commands = new HashMap<>();
    /**
     * Список, сохраняющий данные о последних командах пользователя
     */
    private static List<String> commandHistory = new ArrayList<>();
    /**
     * Объект класса, управляющего считыванием примитивов во время заполнения
     * полей нового элемента коллекции
     */
    private ArgumentsListener argumentsListener = new ArgumentsListener();
    /**
     * Объект коллекции, с полем dragons которого производятся действия
     */
    private CollectionOfDragons collection;

    public CommandListener(CollectionOfDragons collection) {
        this.collection = collection;
        for (Method method : CommandListener.class.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Command.class)) {
                Command command = method.getAnnotation(Command.class);
                commands.put(command.name(), method);
            }
        }
    }

    @Command(name = "help",
            args = "",
            countOfArgs = 0,
            desc = "Доступные пользователю команды",
            aliases = {})
    private void help() {
        StringBuilder sb = new StringBuilder("Список команд: \n");
        for (Method m : this.getClass().getDeclaredMethods()) {
            if (m.isAnnotationPresent(Command.class)) {
                Command com = m.getAnnotation(Command.class);
                sb.append(com.name()).append(" ")
                        .append(com.args()).append(" - ")
                        .append(com.desc()).append("\n");
            }
        }
        System.out.println(sb);
    }

    @Command(name = "info",
            args = "",
            countOfArgs = 0,
            desc = "Вывести информацию о коллекции",
            aliases = {})
    private void info() {
        System.out.println("Information about collection: ");
        collection.showInfo();
    }

    @Command(name = "show",
            args = "",
            countOfArgs = 0,
            desc = "Показать всех драконов в коллекции",
            aliases = {})
    private void show() {
        System.out.println(collection.getDragons());
    }

    @Command(name = "add",
            args = "{name age wingspan}",
            countOfArgs = Dragon.COUNT_OF_PRIMITIVE_ARGS,
            desc = "Добавить элемент в коллекцию",
            aliases = {})
    private void add(String dragonName, String age, String wingspan) {
        String name = dragonName.substring(0, 1).toUpperCase() + dragonName.substring(1); //Делаем имя с большой буквы
        Dragon dragon = new Dragon();
        try {
            dragon.setAge(Integer.parseInt(age));
            dragon.setWingspan(Integer.parseInt(wingspan));
        } catch (NumberFormatException e) {
            System.out.println("Аргументы имеют неверный формат");
            return;
        }
        dragon.setName(name);
        dragon.setCoordinates(argumentsListener.inputCoordinates());
        argumentsListener.inputColor(dragon);
        argumentsListener.inputCharacter(dragon);
        dragon.setCave(argumentsListener.inputCave());
        collection.addDragon(dragon);
    }

    @Command(name = "update",
            args = "{id}",
            countOfArgs = 1,
            desc = "Обновить данные о элементе коллекции по данному id",
            aliases = {})
    private void update(String id) {
        long newId = Long.parseLong(id);
        for (Dragon elem : collection.getDragons()) {
            if (elem.getId() == newId) {
                System.out.println("Введите информацию о драконе: {name, age, wingspan}");
                Scanner sc = new Scanner(System.in);
                argumentsListener.inputPrimitives(elem);
                elem.setCoordinates(argumentsListener.inputCoordinates());
                argumentsListener.inputColor(elem);
                argumentsListener.inputCharacter(elem);
                elem.setCave(argumentsListener.inputCave());
                System.out.println("Данные о драконе успешно обновлены");
            }
        }
    }

    @Command(name = "remove_by_id",
            args = "{id}",
            countOfArgs = 1,
            desc = "Удалить элемент из коллекции по его ID",
            aliases = {})
    private void removeById(String id) {
        collection.removeById(Long.parseLong(id));
    }

    @Command(name = "clear",
            args = "",
            countOfArgs = 0,
            desc = "Очищение коллекции",
            aliases = {})
    private void clear() {
        collection.clear();
        if (collection.getDragons().isEmpty()) {
            System.out.println("Collection successful cleared");
        } else {
            System.out.println("Something went wrong, try again.");
        }
    }

    @Command(name = "save",
            args = "",
            countOfArgs = 0,
            desc = "Сохранение коллекции в файл",
            aliases = {})
    private void save() throws IOException {
        XMLWriter writer = new XMLWriter();
        writer.write(collection.getOutFile(), collection);
        System.out.println("Коллекция успешно сохранена");
    }

    @Command(name = "execute_script",
            args = "{filename}",
            countOfArgs = 1,
            desc = "Считать и исполнить скрипт из указанного файла",
            aliases = {})
    private void executeScript(String filename) {
        try {
            File starting = new File(System.getProperty("user.dir")); // Get current user directory
            File file = new File(starting, filename); // Initialize file from cmd
            System.out.println(file.getPath());
            Scanner sc = new Scanner(file);
            while (sc.hasNext()) {
                ArrayList<String> line = LineSplitter.smartSplit(sc.nextLine());
                invokeMethod(getCommandName(line), getCommandArguments(line));
            }
        } catch (FileNotFoundException e) {
            System.out.println("Файла с таким именем в текущей папке нет. Переместите файл и повторите попытку");
        }
    }

    @Command(name = "exit",
            args = "",
            countOfArgs = 0,
            desc = "Выход из программы без сохранения",
            aliases = {})
    private void exit() throws IOException {
        System.out.println("Сохранить коллекцию в файл? y/n");
        Scanner sc = new Scanner(System.in);
        if (sc.nextLine().equals("y")) {
            save();
        }
        System.exit(0);
    }

    @Command(name = "add_if_max",
            args = "{name, age, wingspan}",
            countOfArgs = Dragon.COUNT_OF_PRIMITIVE_ARGS,
            desc = "Добавить дракона в коллекцию, если его возраст больше, чем у самого старшего в коллекции",
            aliases = {})
    private void addIfMax(String name, String age, String wingspan) {
        int maxAge = 0;
        for (Dragon dragon : collection.getDragons()) {
            if (dragon.getAge() > maxAge) {
                maxAge = dragon.getAge();
            }
        }
        if (Integer.parseInt(age) > maxAge) {
            add(name, age, wingspan);
        } else {
            System.out.println("В коллекции есть дракон постарше!");
        }
    }

    @Command(name = "add_if_min",
            args = "{name, age, wingspan}",
            countOfArgs = Dragon.COUNT_OF_PRIMITIVE_ARGS,
            desc = "Добавить дракона в коллекцию, если его возраст меньше, чем у самого младшего в коллекции",
            aliases = {})
    private void addIfMin(String name, String age, String wingspan) {
        int minAge = Integer.MAX_VALUE;
        for (Dragon dragon : collection.getDragons()) {
            if (dragon.getAge() < minAge) {
                minAge = dragon.getAge();
            }
        }
        if (Integer.parseInt(age) < minAge) {
            add(name, age, wingspan);
        } else {
            System.out.println("В коллекции есть дракон помладше!");
        }
    }

    @Command(name = "history",
            args = "",
            countOfArgs = 0,
            desc = "Вывести последние 11 команд (без их аргументов)",
            aliases = {})
    private void showHistory() {
        final int countOfWatchableCommands = 11;
        if (commandHistory.size() > countOfWatchableCommands) {
            System.out.println(commandHistory.subList(commandHistory.size() - countOfWatchableCommands, commandHistory.size()));
        }
        System.out.println(commandHistory);
    }

    @Command(name = "max_by_cave",
            args = "",
            countOfArgs = 0,
            desc = "Вывести любого дракона из коллекции, глубина пещеры которого является максимальной",
            aliases = {})
    private void showMaxByCave() {
        double maxDepth = Double.MIN_VALUE;
        Dragon dragonWithDeepestCave = new Dragon();
        for (Dragon dragon : collection.getDragons()) {
            if (dragon.getCave().getDepth() > maxDepth) {
                maxDepth = dragon.getCave().getDepth();
                dragonWithDeepestCave = dragon;
            }
        }
        System.out.println("Данные о драконе с самой глубокой пещерой:\n" + dragonWithDeepestCave);
    }

    @Command(name = "print_ascending",
            args = "",
            countOfArgs = 0,
            desc = "Вывести драконов коллекции от младшего к старшему",
            aliases = {})
    private void printAscending() {
        List<Dragon> dragons = new ArrayList<>(collection.getDragons());
        Collections.sort(dragons);
        System.out.println(dragons);
    }

    @Command(name = "print_descending",
            args = "",
            countOfArgs = 0,
            desc = "Вывести драконов коллекции от старшего к младшему",
            aliases = {})
    private void printDescending() {
        List<Dragon> dragons = new ArrayList<>(collection.getDragons());
        dragons.sort(Collections.reverseOrder());
        System.out.println(dragons);
    }

    /**
     * Метод, циклически считывающий команды из консоли и вызывающий необходимые методы обработки коллекции
     */
    public void commandsReader() {
        while (true) { // цикл завершится только при вызове команды exit
            ArrayList<String> line = readCommandFromSystemIn();
            invokeMethod(getCommandName(line), getCommandArguments(line));
        }
    }

    /**
     * Метод, вызывающий необходимую команду по ее имени и аргументам
     *
     * @param commandName название вызываемой команды
     * @param commandArgs аргументы вызываемой команды
     */
    protected void invokeMethod(String commandName, ArrayList<String> commandArgs) {
        Method method = commands.get(commandName);
        commandHistory.add(commandName);
        try {
            Command command = method.getAnnotation(Command.class);
            if (commandArgs.size() != command.countOfArgs()) {
                System.out.println("Неверное количество аргументов. Необходимо: " + command.countOfArgs());
            } else {
                method.invoke(this, commandArgs.toArray());
            }
        } catch (NullPointerException e) {
            System.out.println("Команда не введена, попробуйте еще раз");
        } catch (IllegalAccessException e) {
            System.out.println("Такой команды не существует. Чтобы посмотреть список доступных команд, напишите help");
        } catch (InvocationTargetException e) {
            System.out.println("Имя не может быть пустым, попробуйте снова");
        }
    }

    /**
     * Метод, считывающий команду из консоли и разделяющий ее на имя и аргументы
     *
     * @return разделенная строка с составляющими частями команды
     */
    protected static ArrayList<String> readCommandFromSystemIn() {
        Scanner scanner = new Scanner(System.in);
        String line = scanner.nextLine().toLowerCase();
        return LineSplitter.smartSplit(line);
    }

    /**
     * Метод, извлекающий из полученного массива строк данные, которые являются аргументами
     *
     * @param line разделенная строка
     * @return массив аргументов
     */
    protected static ArrayList<String> getCommandArguments(ArrayList<String> line) {
        line.remove(0);
        return line;
    }

    /**
     * Метод, извлекающий из полученного массива строк имя команды
     *
     * @param line разделенная строка
     * @return имя команды
     */
    protected static String getCommandName(ArrayList<String> line) {
        return line.get(0);
    }
}
