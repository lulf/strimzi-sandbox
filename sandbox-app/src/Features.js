/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
import React, { Component } from 'react';
import './App.css';

class Features extends Component {
  render() {
      return (
            <div>
              <h3>Supported features</h3>
              <ul>
              <li>A <b>single</b> address space of the <i>standard</i> address space type.</li>
              <li>Up to 4 client connections.</li>
              <li>Up to 2 senders per connection.</li>
              <li>Up to 2 receivers per connection.</li>
              <li>Up to 10 anycast addresses.</li>
              <li>Up to 5 multicast addresses.</li>
              <li>Up to 2 queue addresses.</li>
              <li>Up to 1 topic addresses.</li>
              <li>Up to 2 subscription addresses.</li>
              <li>No authentication of messaging clients.</li>
              </ul>
              <p>NOTE: The total number of addresses allowed depends on the type and the mix of address types.</p>
        </div>);
  }
};

export default Features;
