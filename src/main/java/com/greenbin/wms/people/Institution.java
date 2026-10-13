package com.greenbin.wms.people;

/** A school, office or other organisation, identified by its NIPT (tax number). */
public class Institution extends User {
    private final String name;
    private final String nipt;

    public Institution(String name, String nipt, String username, String password, String email,
                       String phone, String address, String community) {
        super(username, password, email, phone, address, community);
        if (!nipt.matches("[A-Z]\\d{8}[A-Z]")) throw new IllegalArgumentException("NIPT must look like K12345678A");
        this.name = name;
        this.nipt = nipt;
    }

    public String getName() { return name; }
    public String getNipt() { return nipt; }

    @Override
    public String getDisplayName() { return name; }
}
