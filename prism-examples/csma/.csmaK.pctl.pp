#const K#

// expected time for all messages to be sent
R{"time"}min=?[ F "all_delivered" ]
R{"time"}max=?[ F "all_delivered" ]

// expected time for one message to be sent
Rmin=?[ F "one_delivered" ]
Rmax=?[ F "one_delivered" ]

// message of any station eventually delivered before i backoffs
#for i=1:K#
Pmin=?[true U "succes_with_backoff_under_#i#" ]
Pmax=?[true U "succes_with_backoff_under_#i#" ]
#end#

// probability all sent successfully before a collision with max backoff 
Pmin=?[ !"collision_max_backoff" U "all_delivered" ]
Pmax=?[ !"collision_max_backoff" U "all_delivered" ]

// probability a station suffers i collisions
#for i=1:K#
Pmin=?[true U "collisions_equal_#i#" ]
Pmax=?[true U "collisions_equal_#i#" ]
#end#


