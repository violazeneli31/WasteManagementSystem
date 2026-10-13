package com.greenbin.wms;

import com.greenbin.wms.collection.LocationType;
import com.greenbin.wms.collection.Vehicle;
import com.greenbin.wms.model.Coordinate;
import com.greenbin.wms.model.WasteType;
import com.greenbin.wms.people.Citizen;
import com.greenbin.wms.people.CompanyManager;
import com.greenbin.wms.people.FinanceOfficer;
import com.greenbin.wms.people.GarbageCollectionEmployee;
import com.greenbin.wms.people.RecyclingFacilityOperator;
import com.greenbin.wms.system.GreenBinSystem;

import java.util.EnumSet;

/** A small, fresh system for each test (not the shared demo instance). */
public class TestData {
    public final GreenBinSystem sys = GreenBinSystem.createEmpty();
    public final CompanyManager manager = new CompanyManager("Viola", "Zeneli", "viola", "password", "1", "v@g.al", 1);
    public final GarbageCollectionEmployee ardi = new GarbageCollectionEmployee("Ardi", "Hoxha", "ardi", "password", "2", "a@g.al", 1, "06-14");
    public final GarbageCollectionEmployee elira = new GarbageCollectionEmployee("Elira", "Kola", "elira", "password", "3", "e@g.al", 1, "06-14");
    public final FinanceOfficer mira = new FinanceOfficer("Mira", "Dervishi", "mira", "password", "4", "m@g.al", 1);
    public final RecyclingFacilityOperator toni = new RecyclingFacilityOperator("Toni", "Gjoka", "toni", "password", "5", "t@g.al", 1, "Plant");
    public final Citizen ana = new Citizen("Ana", "Marku", "ana", "password", "ana@mail.al", "6", "Rr. Kavajes 10", "Blloku");
    public final Citizen ben = new Citizen("Ben", "Leka", "ben", "password", "ben@mail.al", "7", "Rr. Durresit 5", "Kombinat");

    public TestData() {
        sys.addEmployee(manager);
        sys.addEmployee(ardi);
        sys.addEmployee(elira);
        sys.addEmployee(mira);
        sys.addEmployee(toni);
        sys.addUser(ana);
        sys.addUser(ben);
        EnumSet<WasteType> all = EnumSet.allOf(WasteType.class);
        sys.addLocation("Depot", LocationType.DEPOT, new Coordinate(41.3425, 19.8060), all);
        sys.addLocation("Skanderbeg Square", LocationType.PICKUP_POINT, new Coordinate(41.3275, 19.8189), all);
        sys.addLocation("Blloku", LocationType.PICKUP_POINT, new Coordinate(41.3197, 19.8166), all);
        sys.addLocation("Kombinat", LocationType.PICKUP_POINT, new Coordinate(41.3196, 19.7834), all);
        sys.addLocation("Pazari i Ri", LocationType.PICKUP_POINT, new Coordinate(41.3302, 19.8259), all);
        sys.getVehicles().add(new Vehicle("AA 123 BB", 80, new Coordinate(41.3425, 19.8060)));
        sys.getVehicles().add(new Vehicle("AB 456 CD", 80, new Coordinate(41.3196, 19.7834)));
    }
}
