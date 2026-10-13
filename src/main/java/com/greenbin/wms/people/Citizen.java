package com.greenbin.wms.people;

/** A private person using the system. */
public class Citizen extends User {
    private final String name;
    private final String surname;

    public Citizen(String name, String surname, String username, String password, String email,
                   String phone, String address, String community) {
        super(username, password, email, phone, address, community);
        this.name = name;
        this.surname = surname;
    }

    public String getName() { return name; }
    public String getSurname() { return surname; }

    @Override
    public String getDisplayName() { return name + " " + surname; }
}
