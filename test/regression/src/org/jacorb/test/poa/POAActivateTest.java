package org.jacorb.test.poa;

/*
 *        JacORB  - a free Java ORB
 *
 *   Copyright (C) 1997-2001  Gerald Brose.
 *
 *   This library is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU Library General Public
 *   License as published by the Free Software Foundation; either
 *   version 2 of the License, or (at your option) any later version.
 *
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *   Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this library; if not, write to the Free
 *   Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

import junit.framework.*;
import junit.extensions.TestSetup;
import org.jacorb.test.common.ORBSetup;
import org.jacorb.test.orb.BasicServerImpl;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import org.omg.PortableServer.*;
import org.omg.PortableServer.POAPackage.*;
import org.omg.CORBA.*;

/**
 * <code>POAActivateTest</code> tests rapid activation and deactivation of
 * objects in order to ensure the threading is correct.
 *
 * @author <a href="mailto:rnc@prismtechnologies.com"></a>
 * @version 1.0
 */
public class POAActivateTest extends TestCase
{
    /**
     * <code>orb</code> is used to obtain the root poa.
     */
    private static org.omg.CORBA.ORB orb = null;


    /**
     * <code>POAActivateTest</code> constructor - for JUnit.
     *
     * @param name a <code>String</code> value
     */
    public POAActivateTest (String name)
    {
        super (name);
    }


    /**
     * <code>suite</code> lists the tests for Junit to run.
     *
     * @return a <code>Test</code> value
     */
    public static Test suite ()
    {
        TestSuite suite = new TestSuite ("POA Activation/Deactivation Tests");
        Setup setup = new Setup( suite );
        ORBSetup osetup = new ORBSetup( setup );

        suite.addTest (new POAActivateTest ("testActivateDeactivate1"));
        suite.addTest (new POAActivateTest ("testActivateDeactivate2"));
        suite.addTest (new POAActivateTest ("testActivateDeactivate3"));

        return osetup;
    }


    /**
     * <code>testActivateDeactivate1</code> tests activating an object without
     * ID (i.e. using a new one each time) and using servant_to_id to obtain
     * the ID to deactivate_the_object.
     */
    public void testActivateDeactivate1 ()
    {
        try
        {
            POA poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));

            poa.the_POAManager().activate();

            BasicServerImpl soi = new BasicServerImpl();

            for (int count=0;count<100;count++)
            {
//                System.out.println("Iteration #"+count+" - activating object");
                poa.activate_object( soi);
//                System.out.println("Iteration #"+count+" - deactivating object");
                poa.deactivate_object(poa.servant_to_id(soi));
            }
        }
        catch( Exception e )
        {
            fail( "unexpected exception: " + e );
        }
    }


    /**
     * <code>testActivateDeactivate2</code> tests activating and deactivating
     * the object using the same ID.
     */
    public void testActivateDeactivate2 ()
    {
        try
        {
            POA poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));

            poa.the_POAManager().activate();

            BasicServerImpl soi = new BasicServerImpl();

            // This will activate it so do deactivate first
            byte []id = poa.servant_to_id( soi );

            for (int count=0;count<100;count++)
            {
                poa.deactivate_object(id);
                poa.activate_object_with_id(id, soi);
            }
        }
        catch( Exception e )
        {
            fail( "unexpected exception: " + e );
        }
    }


    /**
     * <code>testActivateDeactivate3</code> tests activating an object using a POA policy
     * of MULTIPLE_ID.
     */
    public void testActivateDeactivate3 ()
    {
        try
        {
            POA rootPoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));

            // create POA
            Policy policies[] = new Policy[3];
            policies[0] = rootPoa.create_id_assignment_policy(
                org.omg.PortableServer.IdAssignmentPolicyValue.SYSTEM_ID);
            policies[1] = rootPoa.create_id_uniqueness_policy(
                org.omg.PortableServer.IdUniquenessPolicyValue.MULTIPLE_ID);
            policies[2] = rootPoa.create_servant_retention_policy(
                org.omg.PortableServer.ServantRetentionPolicyValue.RETAIN);

            POA poa = rootPoa.create_POA("system_id", rootPoa.the_POAManager(), policies);

            BasicServerImpl soi = new BasicServerImpl();

            byte [] id = poa.activate_object(soi);

            for (int count=0;count<100;count++)
            {
                poa.deactivate_object(id);
                poa.activate_object_with_id( id, soi);
           }
        }
        catch( Exception e )
        {
            e.printStackTrace();
            fail( "unexpected exception: " + e );
        }
    }




    /**
     * <code>Setup</code> is an inner class to initialize the ORB.
     */
    private static class Setup extends TestSetup
    {
        /**
         * Creates a new <code>Setup</code> instance.
         *
         * @param test a <code>Test</code> value
         */
        public Setup (Test test)
        {
            super (test);
        }

        /**
         * <code>setUp</code> sets the orb variable.
         */
        protected void setUp ()
        {
            org.omg.CORBA.Object obj = null;

            orb = ORBSetup.getORB ();
        }

        /**
         * <code>tearDown</code> does nothing for this test.
         */
        protected void tearDown ()
        {
        }
    }
}
