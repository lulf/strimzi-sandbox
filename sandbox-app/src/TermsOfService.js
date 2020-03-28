import React, { Component } from 'react';
import './App.css';

class TermsOfService extends Component {
  render() {
      return (
              <div className="App">
              <h3>Note</h3>
              <ul>
              <li>The service is intended for <b>testing purposes only</b>. Under no circumstances should it be used for any production use case.</li>
              <li>It is <b>not allowed</b> to register with or publish any personally identifiable information to any of the sandbox services.</li>
              <li>We do not collect nor share with third parties any of the data you provide when registering.</li>
              <li>The sandbox will be running the latest EnMasse master build that may be updated without further notice.</li>
              <li>Everyone who knows your tenant identifier will be able to produce and consume data to your instance.</li>
              </ul>
        </div>);
  }
};

export default TermsOfService;
