package edu.umd.hcil.impressionistpainter434;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import java.text.MessageFormat;
import java.util.Random;

/**
 * Created by jon on 3/20/2016.
 */
public class ImpressionistView extends View {

    private ImageView _imageView;

    Canvas _offScreenCanvas = null;
    Bitmap _offScreenBitmap = null;
    private Paint _paint = new Paint();

    private int _alpha = 150;
    private int _defaultRadius = 25;
    private Point _lastPoint = null;
    private long _lastPointTime = -1;
    private boolean _useMotionSpeedForBrushStrokeSize = true;
    private Paint _paintBorder = new Paint();
    private Paint _paintEraser = new Paint();
    private BrushType _brushType = BrushType.Square;
    private float _minBrushRadius = 5;

    Bitmap texture = null;

    public ImpressionistView(Context context) {
        super(context);
        init(null, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * Because we have more than one constructor (i.e., overloaded constructors), we use
     * a separate initialization method
     * @param attrs
     * @param defStyle
     */
    private void init(AttributeSet attrs, int defStyle){

        // Set setDrawingCacheEnabled to true to support generating a bitmap copy of the view (for saving)
        // See: http://developer.android.com/reference/android/view/View.html#setDrawingCacheEnabled(boolean)
        //      http://developer.android.com/reference/android/view/View.html#getDrawingCache()
        this.setDrawingCacheEnabled(true);

        _paint.setColor(Color.RED);
        _paint.setAlpha(_alpha);
        _paint.setAntiAlias(true);
        _paint.setStyle(Paint.Style.FILL);
        _paint.setStrokeWidth(4);

        _paintBorder.setColor(Color.BLACK);
        _paintBorder.setStrokeWidth(3);
        _paintBorder.setStyle(Paint.Style.STROKE);
        _paintBorder.setAlpha(50);

        this._paintEraser.setColor(0xFFFFFFFF);
        this._paintEraser.setAlpha(50);

        //_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){

        Bitmap bitmap = getDrawingCache();
        Log.v("onSizeChanged", MessageFormat.format("bitmap={0}, w={1}, h={2}, oldw={3}, oldh={4}", bitmap, w, h, oldw, oldh));
        if(bitmap != null) {
            _offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
            _offScreenCanvas = new Canvas(_offScreenBitmap);
        }
    }

    /**
     * Sets the ImageView, which hosts the image that we will paint in this view
     * @param imageView
     */

    public void setImageView(ImageView imageView){
        _imageView = imageView;

    }
    void updateTexture(){

        Rect rect = this.getBitmapPositionInsideImageView(this._imageView);
        this.texture = Bitmap.createScaledBitmap(((BitmapDrawable) this._imageView.getDrawable()).getBitmap(), rect.width(), rect.height(), false);
        this._offScreenBitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888);
        this._offScreenCanvas=new Canvas(this._offScreenBitmap);
        this.clearPainting();
    }
    /**
     * Sets the brush type. Feel free to make your own and completely change my BrushType enum
     * @param brushType
     */
    public void setBrushType(BrushType brushType){
        _brushType = brushType;
    }

    /**
     * Clears the painting
     */
    public void clearPainting(){
        this._offScreenBitmap.eraseColor(0xFFFFFFFF);
        this.invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        try {
            //if (_offScreenBitmap != null) {
                Rect rect = this.getBitmapPositionInsideImageView(this._imageView);
                canvas.drawBitmap(_offScreenBitmap, rect.left, rect.top, _paint);
            //}
        }catch (Exception e){

        }
        /*
        if(MainActivity.texture!=null){
            this._paint.setAlpha(50);
            canvas.drawCircle(20, 20, 50, _paint);
            Rect rect = this.getBitmapPositionInsideImageView(this._imageView);
            canvas.drawBitmap( this.texture, rect.left, rect.top, _paint);
        }
       */
        // Draw the border. Helpful to see the size of the bitmap in the ImageView
        // -------------------------------------------------------------------------- //
         canvas.drawRect(getBitmapPositionInsideImageView(_imageView), _paintBorder);
    }
    float oldX=-1;
    float oldY=-1;
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){

        //Basically, the way this works is to liste for Touch Down and Touch Move events and determine where those
        //touch locations correspond to the bitmap in the ImageView. You can then grab info about the bitmap--like the pixel color--
        //at that location
        try {
            float x = motionEvent.getX();
            float y = motionEvent.getY();

            int type = motionEvent.getAction();
           // Bitmap bitmap = ((BitmapDrawable) this._imageView.getDrawable()).getBitmap();
            Rect rect = this.getBitmapPositionInsideImageView(this._imageView);
            if (type == MotionEvent.ACTION_MOVE) {
                int mapX = (int) (x-rect.left);
                int mapY = (int) (y-rect.top);
                int color = this.texture.getPixel(mapX, mapY);
                float size = 50;
                this._paint.setColor(color);
                if(this._brushType==BrushType.Circle) {
                    float circleRadius = (float) (size / 1.25);
                    this._offScreenCanvas.drawCircle(mapX, mapY, circleRadius, _paint);
                }else if(this._brushType==BrushType.Square){
                    float rectSize = (float) (size * 1.25);
                    this._offScreenCanvas.drawRect(mapX - rectSize, mapY -rectSize, mapX + rectSize, mapY + rectSize, _paint);
                }else if(this._brushType==BrushType.Line){
                    if(this.oldX!=-1){
                        float thickness = Math.abs(mapX-this.oldX)+Math.abs(mapY-this.oldY);
                        this._paint.setStrokeWidth(thickness);
                        this._offScreenCanvas.drawLine(this.oldX,this.oldY,mapX,mapY,this._paint);
                    }
                    this.oldX=mapX;
                    this.oldY=mapY;
                }else if(this._brushType==BrushType.LineSplatter){
                    float circleRadius = size;

                    this._offScreenCanvas.drawCircle(mapX, mapY, circleRadius, _paintEraser);
                }

            } else if (type == MotionEvent.ACTION_UP) {
                this.oldX=-1;
            }
            this.invalidate();
        }catch(Exception e){
            return true;
        }
        return true;
    }




    /**
     * This method is useful to determine the bitmap position within the Image View. It's not needed for anything else
     * Modified from:
     *  - http://stackoverflow.com/a/15538856
     *  - http://stackoverflow.com/a/26930938
     * @param imageView
     * @return
     */
    private static Rect getBitmapPositionInsideImageView(ImageView imageView){
        Rect rect = new Rect();

        if (imageView == null || imageView.getDrawable() == null) {
            return rect;
        }

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int widthActual = Math.round(origW * scaleX);
        final int heightActual = Math.round(origH * scaleY);

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - heightActual)/2;
        int left = (int) (imgViewW - widthActual)/2;

        rect.set(left, top, left + widthActual, top + heightActual);

        return rect;
    }
}

