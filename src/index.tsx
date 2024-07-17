import { NativeModules } from 'react-native';

type BillieCameraType = {
  startCamera(type: number): Promise<string>;
};

const { BillieCamera } = NativeModules;

export default BillieCamera as BillieCameraType;
