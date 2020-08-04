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
              <h3>Features</h3>
              <ul>
              <li>A <b>single</b> shared Kafka cluster.</li>
              <li>Up to 2 topics.</li>
              <li>TLS required</li>
              </ul>
        </div>);
  }
};

export default Features;
