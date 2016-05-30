package producerconsumer.router;

import java.util.ArrayList;
import java.util.Random;

public class TestPacketFactory {

    static ArrayList<Packet> makeEqualDistribution(int size) {

        int managementCount = 0;
        int managementLarge = 0;
        int user = 0;
        int userLarge = 0;

        // cache the packets up front for testing
        ArrayList<Packet> cachedList = new ArrayList<>();
        for (int pCount = 0; pCount<size; pCount++) {
            Packet p = new Packet();

            if (pCount% 5 == 0) {
                p.setPacketType(Packet.PacketType.Management).setLarge(true);
                managementLarge++;
            }
            else if (pCount% 3 == 0) {
                p.setPacketType(Packet.PacketType.Management).setLarge(false);
                managementCount++;
            }
            else if (pCount % 2 == 0) {
                p.setPacketType(Packet.PacketType.User).setLarge(false);
                userLarge++;
            }
            else {
                p.setPacketType(Packet.PacketType.User).setLarge(false);
                user++;
            }

            cachedList.add(p);
        }

        System.out.println("Distribution details: (Static #) :  "+ cachedList.size()+", mgmt large: "+managementLarge+", mgmt: "+managementCount+
                ", userLarge: "+userLarge+", user: "+user);

        return cachedList;
    }


    static ArrayList<Packet> makeRandomDistribution(int size)
    {

        int managementCount = 0;
        int managementLarge = 0;
        int user = 0;
        int userLarge = 0;

        // could do better, but ok.  Nice property that you end up with none for any given type a fair amount.
        double runningTotal = 0;
        double pickMgmtLarge = new Random(System.currentTimeMillis()).nextDouble() * size;
        runningTotal += pickMgmtLarge;
        double pickMgmt = new Random(10*System.currentTimeMillis()).nextDouble() * (size-runningTotal);
        runningTotal += pickMgmt;
        double pickUserLarge = new Random(100* System.currentTimeMillis()).nextDouble()*(size-runningTotal) ;
        double pickUser = size - runningTotal;

        ArrayList<Packet> randomDist = new ArrayList<>();
        for (int pCount=0; pCount<size; pCount++) {
            // slow but not part of the test
            Packet p = new Packet();

            if (pCount < pickMgmtLarge) {
                p.setPacketType(Packet.PacketType.Management).setLarge(true);
                managementLarge++;
            }
            else if (pCount < pickMgmt) {
                p.setPacketType(Packet.PacketType.Management).setLarge(false);
                managementCount++;
            }
            else if (pCount < pickUserLarge) {
                p.setPacketType(Packet.PacketType.User).setLarge(true);
                userLarge++;
            }
            else {
                p.setPacketType(Packet.PacketType.User).setLarge(false);
                user++;
            }

            randomDist.add(p);
        }

        System.out.println("Distribution details:(RANDOM #)  :"+ randomDist.size()+", mgmt large: "+managementLarge+", mgmt: "+managementCount+
                ", userLarge: "+userLarge+", user: "+user);

        return randomDist;
    }
}
